"""Restore only this run's immutable payload; never rebuild after tagging."""

import os
import subprocess
from pathlib import Path

from release_common import REPOSITORY, candidate, gh, output, require
from release_payload import verify


def restore() -> None:
    release = candidate()
    name = "release-payload-" + release["sha"]
    run_id = os.environ["GITHUB_RUN_ID"]
    artifacts = gh(f"repos/{REPOSITORY}/actions/runs/{run_id}/artifacts?per_page=100")[
        "artifacts"
    ]
    matches = [artifact for artifact in artifacts if artifact["name"] == name]
    if matches:
        require(
            len(matches) == 1 and not matches[0]["expired"],
            "Retained release payload expired or ambiguous",
        )
        subprocess.run(
            [
                "gh",
                "run",
                "download",
                run_id,
                "--repo",
                REPOSITORY,
                "--name",
                name,
                "--dir",
                "target/release-payload",
            ],
            check=True,
        )
        verify(Path("target/release-payload"), release)
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


if __name__ == "__main__":
    restore()
