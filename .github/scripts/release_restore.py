"""Keep the sealed payload in a draft vault; never rebuild after tagging.

The vault is a draft release, `release-payload-SHA`, that is never published.
Its only asset is the sealed payload archive. Draft assets do not expire, and
no workflow rerun discards them, unlike the Actions artifacts of a rerun job.
"""

import argparse
import io
import os
import subprocess
import zipfile
from pathlib import Path, PurePosixPath

from release_common import (
    REPOSITORY,
    JsonObject,
    Release,
    candidate,
    fetch,
    gh,
    output,
    require,
)
from release_payload import verify, zip_files

ROOT = Path("target/release-payload")
ARCHIVES = Path("target/release-vault")


def vault_tag(sha: str) -> str:
    return "release-payload-" + sha


def vault_record(sha: str) -> JsonObject | None:
    records = gh(f"repos/{REPOSITORY}/releases?per_page=100")
    matching = [record for record in records if record["tag_name"] == vault_tag(sha)]
    require(len(matching) <= 1, "Multiple payload vaults")
    if not matching:
        return None
    record = matching[0]
    require(
        record["draft"] and f"source: {sha}" in (record["body"] or ""),
        "Payload vault identity mismatch",
    )
    return record


def vault_asset(record: JsonObject, sha: str) -> JsonObject | None:
    assets = record["assets"]
    require(
        len(assets) <= 1
        and all(asset["name"] == vault_tag(sha) + ".zip" for asset in assets),
        "Unexpected payload vault assets",
    )
    return assets[0] if assets else None


def sealed_asset(sha: str) -> JsonObject | None:
    record = vault_record(sha)
    asset = vault_asset(record, sha) if record is not None else None
    return asset if asset is not None and asset.get("state") == "uploaded" else None


def download(asset: JsonObject) -> bytes:
    return subprocess.check_output(
        [
            "gh",
            "api",
            f"repos/{REPOSITORY}/releases/assets/{asset['id']}",
            "-H",
            "Accept: application/octet-stream",
        ]
    )


def upload(record: JsonObject, archive: Path) -> None:
    token = os.environ.get("GH_TOKEN")
    require(token, "Missing GitHub token")
    # Upload by release ID: the vault has no tag for tag-based lookups.
    fetch(
        f"https://uploads.github.com/repos/{REPOSITORY}/releases/{int(record['id'])}"
        f"/assets?name={archive.name}",
        {
            "Accept": "application/vnd.github+json",
            "Authorization": "Bearer " + str(token),
            "Content-Type": "application/zip",
            "X-GitHub-Api-Version": "2022-11-28",
        },
        archive.read_bytes(),
        "POST",
    )


def pack(release: Release) -> Path:
    archive = ARCHIVES / (vault_tag(release["sha"]) + ".zip")
    archive.parent.mkdir(parents=True, exist_ok=True)
    files = [path for path in ROOT.rglob("*") if path.is_file()]
    zip_files(archive, files, ROOT, release["epoch"])
    return archive


def unpack(data: bytes) -> None:
    require(not ROOT.exists(), "Refusing to replace the retained payload")
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for entry in archive.infolist():
            name = PurePosixPath(entry.filename)
            require(
                not entry.is_dir()
                and name.parts
                and not name.is_absolute()
                and ".." not in name.parts,
                "Unsafe payload vault entry",
            )
            path = ROOT.joinpath(*name.parts)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(archive.read(entry))


def restore(release: Release) -> None:
    asset = sealed_asset(release["sha"])
    if asset is not None:
        unpack(download(asset))
        verify(ROOT, release)
        print("Restored the sealed payload from its draft vault; skipping the build.")
        output(
            {
                "restored": "true",
                "needs-signing": "false" if release["retry"] else "true",
            }
        )
        return
    require(
        not release["retry"],
        "A tag exists but original bytes are unavailable; do not rebuild",
    )
    output({"restored": "false", "needs-signing": "true"})


def retain(release: Release) -> None:
    verify(ROOT, release)
    archive = pack(release)
    sha = release["sha"]
    record = vault_record(sha)
    if record is None:
        gh(
            f"repos/{REPOSITORY}/releases",
            "POST",
            {
                "tag_name": vault_tag(sha),
                "target_commitish": sha,
                "name": f"{release['tag']} sealed payload (never publish)",
                "body": (
                    f"Sealed payload retained for {release['tag']} recovery. "
                    "Never publish or edit this draft; finalization deletes it.\n"
                    f"<!-- source: {sha} -->\n"
                ),
                "draft": True,
            },
        )
        record = vault_record(sha)
        require(record is not None, "Payload vault was not created")
    asset = vault_asset(record, sha)
    if asset is not None and asset.get("state") != "uploaded":
        # An interrupted upload leaves an unusable asset; only an untagged run has one.
        require(not release["retry"], "Incomplete payload vault after tagging")
        gh(f"repos/{REPOSITORY}/releases/assets/{asset['id']}", "DELETE")
        asset = None
    if asset is None:
        upload(record, archive)
        asset = sealed_asset(sha)
    require(
        asset is not None and download(asset) == archive.read_bytes(),
        "Payload vault differs from the sealed payload; never overwrite",
    )
    print(f"Sealed payload retained in draft vault {vault_tag(sha)}.")


def retrieve(sha: str) -> None:
    asset = sealed_asset(sha)
    require(asset is not None, "Sealed payload vault is missing")
    unpack(download(asset))
    release = candidate(ROOT / "release-candidate.json")
    require(release["sha"] == sha, "Retained payload source mismatch")
    verify(ROOT, release)


def discard(release: Release) -> None:
    record = vault_record(release["sha"])
    if record is not None:
        gh(f"repos/{REPOSITORY}/releases/{record['id']}", "DELETE")
        print(f"Deleted draft vault {vault_tag(release['sha'])}.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("restore", "retain", "retrieve"))
    args = parser.parse_args()
    if args.command == "retrieve":
        retrieve(os.environ["GITHUB_SHA"])
    elif args.command == "retain":
        retain(candidate())
    else:
        restore(candidate())
