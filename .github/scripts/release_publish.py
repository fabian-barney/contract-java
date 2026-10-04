"""Publish only retained, verified bytes; keep partial releases recoverable."""

import argparse
import base64
import json
import os
import re
import subprocess
import time
import uuid
from pathlib import Path

from release_common import (
    ARTIFACTS,
    CENTRAL,
    FINGERPRINT,
    REPOSITORY,
    JsonObject,
    Release,
    candidate,
    fetch,
    gh,
    git,
    maven_path,
    require,
    run,
)
from release_control import verify_tag
from release_payload import verify

ROOT = Path("target/release-payload")
PORTAL = "https://central.sonatype.com/api/v1/publisher"


def release_record(release: Release) -> JsonObject | None:
    records = gh(f"repos/{REPOSITORY}/releases?per_page=100")
    matching = [record for record in records if record["tag_name"] == release["tag"]]
    require(len(matching) <= 1, "Multiple release records")
    if matching:
        require(
            f"source: {release['sha']}" in matching[0]["body"],
            "Release source mismatch",
        )
        return matching[0]
    return None


def tag(release: Release) -> None:
    require(git("rev-parse", "HEAD") == release["sha"], "Tag source mismatch")
    existing = git("ls-remote", "origin", f"refs/tags/{release['tag']}")
    if existing:
        verify_tag(release["tag"], release["sha"])
        return
    git("config", "user.name", "github-actions[bot]")
    git("config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")
    git("config", "user.signingkey", FINGERPRINT)
    git(
        "config",
        "gpg.program",
        str(Path(".github/scripts/gpg-release-sign.sh").resolve()),
    )
    environment = dict(
        os.environ,
        GIT_COMMITTER_DATE=release["timestamp"],
        RELEASE_EPOCH=str(release["epoch"]),
    )
    run(
        "git",
        "tag",
        "--sign",
        "--message",
        f"Release {release['tag']}",
        release["tag"],
        release["sha"],
        env=environment,
    )
    verify_tag(release["tag"], release["sha"])
    git("push", "origin", f"refs/tags/{release['tag']}")


def draft(release: Release) -> None:
    record = release_record(release)
    if record is None:
        notes = release["notes"] + f"\n<!-- source: {release['sha']} -->\n"
        record = gh(
            f"repos/{REPOSITORY}/releases",
            "POST",
            {
                "tag_name": release["tag"],
                "target_commitish": release["sha"],
                "name": release["tag"],
                "body": notes,
                "draft": True,
            },
        )
    require(
        record["draft"], "Release is already public; use verification-only recovery"
    )
    for path in sorted((ROOT / "github").iterdir()):
        existing = [asset for asset in record["assets"] if asset["name"] == path.name]
        if existing:
            downloaded = subprocess.check_output(
                [
                    "gh",
                    "api",
                    f"repos/{REPOSITORY}/releases/assets/{existing[0]['id']}",
                    "-H",
                    "Accept: application/octet-stream",
                ]
            )
            require(
                downloaded == path.read_bytes(),
                "Conflicting GitHub asset; never overwrite",
            )
        else:
            run(
                "gh",
                "release",
                "upload",
                release["tag"],
                str(path),
                "--repo",
                REPOSITORY,
            )
    github_assets(release)


def github_assets(release: Release) -> None:
    record = release_record(release)
    local = {path.name: path for path in (ROOT / "github").iterdir()}
    require(
        record is not None
        and {asset["name"] for asset in record["assets"]} == set(local),
        "GitHub asset set mismatch",
    )
    for asset in record["assets"]:
        downloaded = subprocess.check_output(
            [
                "gh",
                "api",
                f"repos/{REPOSITORY}/releases/assets/{asset['id']}",
                "-H",
                "Accept: application/octet-stream",
            ]
        )
        require(
            downloaded == local[asset["name"]].read_bytes(),
            "GitHub download differs from retained bytes",
        )


def payloads() -> list[Path]:
    return sorted(
        path
        for path in (ROOT / "central").rglob("*")
        if path.suffix in (".pom", ".jar", ".json", ".xml")
    )


def attestations(release: Release) -> None:
    common = [
        "--repo",
        REPOSITORY,
        "--signer-workflow",
        f"{REPOSITORY}/.github/workflows/release.yml",
        "--source-digest",
        release["sha"],
        "--source-ref",
        "refs/heads/main",
        "--deny-self-hosted-runners",
    ]
    for path in payloads() + [
        ROOT / "github" / "SHA256SUMS",
        ROOT / "github" / "SHA512SUMS",
    ]:
        run("gh", "attestation", "verify", str(path), *common)
    for artifact in ARTIFACTS:
        prefix = ROOT / "central" / maven_path(artifact, release["version"])
        subject = str(prefix) + (".pom" if artifact == "contract-java" else ".jar")
        verified = json.loads(
            run(
                "gh",
                "attestation",
                "verify",
                subject,
                *common,
                "--predicate-type",
                "https://cyclonedx.org/bom",
                "--format",
                "json",
            )
        )
        expected = json.loads(Path(str(prefix) + "-cyclonedx.json").read_text())
        require(
            any(
                item["verificationResult"]["statement"]["predicate"] == expected
                for item in verified
            ),
            "Attested SBOM differs from the retained SBOM",
        )


def central_downloads(required: bool = True) -> tuple[bool, bool]:
    found = []
    for path in sorted((ROOT / "central").rglob("*")):
        if not path.is_file():
            continue
        remote = fetch(
            f"{CENTRAL}/{path.relative_to(ROOT / 'central').as_posix()}", missing=True
        )
        if remote is None:
            require(not required, "Central artifact not yet downloadable")
        else:
            require(
                remote == path.read_bytes(),
                "Central bytes conflict with retained payload",
            )
        found.append(remote is not None)
    return all(found), any(found)


def portal_headers() -> dict[str, str]:
    username = os.environ.get("MAVEN_CENTRAL_TOKEN_USERNAME")
    password = os.environ.get("MAVEN_CENTRAL_TOKEN_PASSWORD")
    require(username and password, "Missing Central publishing credentials")
    token = base64.b64encode(f"{username}:{password}".encode()).decode()
    return {"Authorization": "Bearer " + token}


def publish_central(release: Release) -> None:
    complete, partial = central_downloads(required=False)
    if complete:
        print("Central already contains the exact retained payload.")
        return
    record = release_record(release)
    require(record and record["draft"], "Central publication requires a draft")
    match = re.search(r"<!-- central-deployment: ([0-9a-f-]{36}) -->", record["body"])
    if match:
        deployment = match[1]
    else:
        require(
            not partial,
            "Partial Central release without deployment ID: recover metadata, never rebuild",
        )
        deployment = upload_bundle(record, release)
    wait_deployment(deployment)


def upload_bundle(record: JsonObject, release: Release) -> str:
    require(
        "<!-- central-upload-started -->" not in record["body"],
        "Previous upload outcome unknown; recover its deployment ID",
    )
    body = record["body"] + "\n<!-- central-upload-started -->\n"
    gh(f"repos/{REPOSITORY}/releases/{record['id']}", "PATCH", {"body": body})
    boundary = "contract-java-" + uuid.uuid4().hex
    bundle = (ROOT / "central-bundle.zip").read_bytes()
    data = (
        (
            f'--{boundary}\r\nContent-Disposition: form-data; name="bundle"; filename="central-bundle.zip"\r\n'
            "Content-Type: application/octet-stream\r\n\r\n"
        ).encode()
        + bundle
        + f"\r\n--{boundary}--\r\n".encode()
    )
    headers = dict(
        portal_headers(),
        **{"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    deployment = (
        fetch(
            f"{PORTAL}/upload?publishingType=AUTOMATIC&name=contract-java-{release['version']}",
            headers,
            data,
        )
        .decode()
        .strip()
        .strip('"')
    )
    require(str(uuid.UUID(deployment)) == deployment, "Invalid Central deployment ID")
    print(f"Central deployment ID: {deployment}")
    gh(
        f"repos/{REPOSITORY}/releases/{record['id']}",
        "PATCH",
        {"body": body + f"\n<!-- central-deployment: {deployment} -->\n"},
    )
    return deployment


def wait_downloads() -> None:
    deadline = time.monotonic() + 900
    while time.monotonic() < deadline:
        if central_downloads(required=False)[0]:
            return
        time.sleep(30)
    raise ValueError("Central published but downloads remain incomplete")


def wait_deployment(deployment: str) -> None:
    deadline = time.monotonic() + 3600
    while time.monotonic() < deadline:
        state = json.loads(
            fetch(f"{PORTAL}/status?id={deployment}", portal_headers(), b"", "POST")
        )["deploymentState"]
        require(
            state in ("PENDING", "VALIDATING", "VALIDATED", "PUBLISHING", "PUBLISHED"),
            f"Central deployment failed: {deployment}",
        )
        if state == "PUBLISHED":
            wait_downloads()
            return
        time.sleep(30)
    raise ValueError(
        f"Central publication timed out: {deployment}; preserve the draft and payload"
    )


def finalize(release: Release) -> None:
    verify(ROOT, release)
    verify_tag(release["tag"], release["sha"])
    central_downloads()
    github_assets(release)
    attestations(release)
    url = f"https://fabian-barney.github.io/contract-java/api/{release['version']}/index.html"
    page = fetch(url).decode()
    require(
        f'data-source-sha="{release["sha"]}"' in page,
        "Versioned Javadoc source mismatch",
    )
    record = release_record(release)
    gh(
        f"repos/{REPOSITORY}/releases/{record['id']}",
        "PATCH",
        {"draft": False, "make_latest": "true"},
    )
    print(
        f"Verified release: https://github.com/{REPOSITORY}/releases/tag/{release['tag']}"
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "command", choices=("tag", "draft", "attestations", "central", "finalize")
    )
    args = parser.parse_args()
    release = candidate()
    if args.command != "tag":
        verify(ROOT, release)
    {
        "tag": tag,
        "draft": draft,
        "attestations": attestations,
        "central": publish_central,
        "finalize": finalize,
    }[args.command](release)
