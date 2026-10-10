"""Preserve the Pages archive and extract the exact published Javadoc JARs."""

import re
import shutil
import zipfile
from pathlib import Path

from release_common import ARTIFACTS, candidate, git, require, version_tuple
from release_payload import verify


def prepare() -> None:
    release = candidate()
    verify("target/release-payload", release)
    site = Path("target/pages")
    if git("ls-remote", "origin", "refs/heads/gh-pages"):
        git("fetch", "origin", "gh-pages")
        git("worktree", "add", str(site), "FETCH_HEAD")
    else:
        git("worktree", "add", "--detach", str(site))
        git("-C", str(site), "checkout", "--orphan", "gh-pages")
        git("-C", str(site), "rm", "-rf", ".")
    destination = site / "api" / release["version"]
    expected_index = f'data-source-sha="{release["sha"]}"'
    if destination.exists():
        require(
            expected_index in (destination / "index.html").read_text(),
            "Existing Javadoc source differs",
        )
    else:
        destination.mkdir(parents=True)
        for artifact in ARTIFACTS[1:]:
            jars = list(
                Path("target/release-payload/central").rglob(
                    f"{artifact}-{release['version']}-javadoc.jar"
                )
            )
            require(len(jars) == 1, "Missing retained Javadoc archive")
            with zipfile.ZipFile(jars[0]) as archive:
                archive.extractall(destination / artifact)
        (destination / "index.html").write_text(
            f'<!doctype html><html lang="en"><meta charset="utf-8"><title>contract-java {release["version"]} API</title>'
            f"<body {expected_index}><h1>contract-java {release['version']} API</h1>"
            '<ul><li><a href="contract-core/index.html">Core</a></li>'
            '<li><a href="contract-spring-boot-starter/index.html">Spring Boot starter</a></li></ul></body></html>\n',
            encoding="utf-8",
        )
    versions = [
        p.name for p in (site / "api").iterdir() if p.is_dir() and p.name != "latest"
    ]
    versions = [
        version for version in versions if re.fullmatch(r"\d+\.\d+\.\d+", version)
    ]
    latest = max(versions, key=version_tuple)
    redirect = site / "api/latest"
    redirect.mkdir(exist_ok=True)
    (redirect / "index.html").write_text(
        f'<!doctype html><html lang="en"><meta charset="utf-8"><title>Latest contract-java API</title>'
        f'<meta http-equiv="refresh" content="0; url=../{latest}/"><link rel="canonical" href="../{latest}/">'
        f'<a href="../{latest}/">contract-java {latest} API</a></html>\n',
        encoding="utf-8",
    )
    (site / ".nojekyll").touch()
    git("-C", str(site), "config", "user.name", "github-actions[bot]")
    git(
        "-C",
        str(site),
        "config",
        "user.email",
        "41898282+github-actions[bot]@users.noreply.github.com",
    )
    git("-C", str(site), "add", "api", ".nojekyll")
    if git("-C", str(site), "diff", "--cached", "--name-only"):
        git(
            "-C",
            str(site),
            "commit",
            "--signoff",
            "--message",
            f"docs: publish Javadoc for {release['version']}",
        )
        git("-C", str(site), "push", "origin", "HEAD:gh-pages")
    shutil.copytree(site, "target/pages-upload", ignore=shutil.ignore_patterns(".git"))


if __name__ == "__main__":
    prepare()
