"""Select exact push candidates and wait for that commit's complete CI gate."""

import argparse
import datetime as dt
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET

from release_common import (
    ARTIFACTS,
    CENTRAL,
    FINGERPRINT,
    NS,
    REPOSITORY,
    JsonObject,
    fetch,
    gh,
    git,
    maven_path,
    output,
    require,
    revision,
    timestamp,
    version_tuple,
    write_json,
)


def transition(previous: str | None, current: str | None) -> bool:
    require(current is not None, "Missing reactor revision")
    if current == previous:
        return False
    if current.endswith("-SNAPSHOT"):
        version_tuple(current.removesuffix("-SNAPSHOT"))
        return False
    version_tuple(current)
    return True


def release_notes(changelog: str, version: str) -> str:
    match = re.search(
        rf"^## {re.escape(version)} - (\d{{4}}-\d{{2}}-\d{{2}})\n(.*?)(?=^## |^\[|\Z)",
        changelog.replace("\r\n", "\n"),
        re.M | re.S,
    )
    require(match is not None, "Missing dated changelog section")
    date = dt.date.fromisoformat(match[1])
    require(
        date <= dt.datetime.now(dt.timezone.utc).date(),
        "Changelog date is in the future",
    )
    require(match[2].strip(), "Empty release notes")
    return match[0].strip() + "\n"


def aligned(sha: str, version: str) -> None:
    root = ET.fromstring(git("show", f"{sha}:pom.xml"))
    for module in root.findall("m:modules/m:module", NS):
        pom = ET.fromstring(git("show", f"{sha}:{module.text}/pom.xml"))
        require(
            pom.findtext("m:parent/m:version", namespaces=NS) == "${revision}",
            "Unaligned parent version",
        )
        own = pom.findtext("m:version", namespaces=NS)
        require(own in (None, "${revision}", version), "Unaligned module version")
        for dependency in pom.findall("m:dependencies/m:dependency", NS):
            if dependency.findtext("m:groupId", namespaces=NS) == "media.barney":
                require(
                    dependency.findtext("m:version", namespaces=NS)
                    in ("${project.version}", "${revision}", version),
                    "Unaligned reactor dependency",
                )


def verify_tag(tag: str, sha: str) -> None:
    require(git("cat-file", "-t", tag) == "tag", "Release tag must be annotated")
    require(
        git("rev-parse", f"{tag}^{{commit}}") == sha,
        "Release tag points to another commit",
    )
    verified = subprocess.run(
        ["git", "verify-tag", "--raw", tag], capture_output=True, text=True
    )
    require(verified.returncode == 0, "Invalid release tag signature")
    signatures = [
        line.split()
        for line in verified.stderr.splitlines()
        if line.startswith("[GNUPG:] VALIDSIG ")
    ]
    require(
        len(signatures) == 1 and FINGERPRINT in (signatures[0][2], signatures[0][-1]),
        "Wrong tag signing fingerprint",
    )
    tag_text = git("cat-file", "tag", tag)
    require("BEGIN PGP SIGNATURE" in tag_text, "Release tag is unsigned")


def select(sha: str) -> None:
    require(
        os.environ.get("GITHUB_REPOSITORY", REPOSITORY) == REPOSITORY,
        "Unexpected repository",
    )
    require(
        os.environ.get("GITHUB_REF", "refs/heads/main") == "refs/heads/main",
        "Publication requires main",
    )
    require(
        gh(f"repos/{REPOSITORY}/branches/main")["protected"], "main must be protected"
    )
    require(git("rev-parse", "HEAD") == sha, "Checkout does not match the push SHA")
    parent = git("rev-parse", f"{sha}^1")
    current = revision(git("show", f"{sha}:pom.xml"))
    previous = revision(git("show", f"{parent}:pom.xml"))
    if not transition(previous, current):
        output({"publish": "false"})
        print("Revision unchanged or snapshot: no publication.")
        return
    aligned(sha, current)
    notes = release_notes(git("show", f"{sha}:CHANGELOG.md"), current)
    releases = gh(f"repos/{REPOSITORY}/releases?per_page=100")
    published = [r for r in releases if not r["draft"] and not r["prerelease"]]
    versions = [
        version_tuple(r["tag_name"][1:])
        for r in published
        if re.fullmatch(r"v\d+\.\d+\.\d+", r["tag_name"])
    ]
    require(
        not versions or version_tuple(current) > max(versions),
        "Version must exceed published releases",
    )
    tag = "v" + current
    existing = [r for r in releases if r["tag_name"] == tag]
    remote = git("ls-remote", "origin", f"refs/tags/{tag}")
    if remote:
        verify_tag(tag, sha)
        # A retry must recover the retained payload before doing any publication work.
    if existing:
        require(
            existing[0]["draft"] and f"source: {sha}" in existing[0]["body"],
            "Conflicting GitHub release",
        )
    if not remote:
        require(not existing, "Release exists without the expected tag")
        for artifact in ARTIFACTS:
            require(
                fetch(f"{CENTRAL}/{maven_path(artifact, current)}.pom", missing=True)
                is None,
                "Maven Central coordinates already exist",
            )
    epoch, instant = timestamp(sha)
    write_json(
        os.environ.get("RELEASE_CANDIDATE", "target/release-candidate.json"),
        {
            "version": current,
            "sha": sha,
            "epoch": epoch,
            "timestamp": instant,
            "tag": tag,
            "notes": notes,
            "retry": bool(remote),
        },
    )
    output({"publish": "true", "version": current, "sha": sha, "epoch": epoch})


def required_state(workflow: JsonObject, jobs: list[JsonObject]) -> bool:
    if workflow["status"] != "completed":
        return False
    require(
        workflow["conclusion"] == "success",
        "Required CI failed, was cancelled, or timed out",
    )
    gates = [job for job in jobs if job["name"] == "verify / required"]
    require(
        len(gates) == 1 and gates[0]["conclusion"] == "success",
        "Required aggregator did not succeed",
    )
    return True


def wait_required(sha: str, timeout: int = 5400) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        runs = gh(
            f"repos/{REPOSITORY}/actions/workflows/ci.yml/runs?head_sha={sha}&event=push&per_page=100"
        )["workflow_runs"]
        runs = [
            run
            for run in runs
            if run["head_sha"] == sha and run["head_branch"] == "main"
        ]
        if runs:
            workflow = max(runs, key=lambda run: run["id"])
            jobs = gh(
                f"repos/{REPOSITORY}/actions/runs/{workflow['id']}/jobs?per_page=100"
            )["jobs"]
            if required_state(workflow, jobs):
                print(f"Exact source CI succeeded: {workflow['html_url']}")
                return
        time.sleep(30)
    raise ValueError("Timed out waiting for exact source CI")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("candidate", "wait"))
    parser.add_argument("sha")
    args = parser.parse_args()
    if args.command == "candidate":
        select(args.sha)
    else:
        wait_required(args.sha)
