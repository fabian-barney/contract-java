"""Two clean unsigned builds, using the same release staging path as publication."""

import os
import subprocess
import tempfile
from pathlib import Path

from release_common import digest, git, require, revision, timestamp
from release_consumer import consumers
from release_payload import stage


def preflight() -> None:
    if os.name != "nt":
        wrapper_path = Path("mvnw")
        wrapper_path.chmod(wrapper_path.stat().st_mode | 0o111)
    sha = git("rev-parse", "HEAD")
    epoch, instant = timestamp(sha)
    current = revision(Path("pom.xml").read_text())
    version = current if not current.endswith("-SNAPSHOT") else "1.0.0"
    release = {"sha": sha, "version": version, "epoch": epoch, "timestamp": instant}
    wrapper = ["cmd", "/c", "mvnw.cmd"] if os.name == "nt" else ["bash", "./mvnw"]
    with tempfile.TemporaryDirectory(prefix="contract-release-preflight-") as temp:
        results = []
        for index in range(2):
            subprocess.run(
                wrapper
                + [
                    "-B",
                    "-ntp",
                    "-Prelease",
                    f"-Drevision={version}",
                    f"-Dscm.tag=v{version}",
                    f"-Dproject.build.outputTimestamp={epoch}",
                    "-Dgpg.skip=true",
                    "-DskipTests",
                    "clean",
                    "verify",
                ],
                check=True,
            )
            target = Path(temp) / str(index)
            stage(release, target)
            results.append(
                {
                    p.relative_to(target).as_posix(): digest(p)
                    for p in target.rglob("*")
                    if p.is_file()
                }
            )
        mismatches = [
            name for name in results[0] if results[0][name] != results[1].get(name)
        ]
        require(
            results[0] == results[1], f"Unsigned builds differ: {', '.join(mismatches)}"
        )
        print(
            f"Two clean builds match: {len(results[0])} unsigned payloads and manifests."
        )
        consumers(version)


if __name__ == "__main__":
    preflight()
