"""Shared, fail-closed release primitives (standard library only)."""

import datetime as dt
import hashlib
import json
import os
import re
import subprocess
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, TypedDict, cast

# Raw GitHub/Portal JSON has endpoint-specific schemas. Validate consumed fields
# at each boundary; Any is confined to these JSON transport objects.
JsonObject = dict[str, Any]


class Release(TypedDict):
    version: str
    sha: str
    epoch: int
    timestamp: str
    tag: str
    notes: str
    retry: bool


REPOSITORY = "fabian-barney/contract-java"
FINGERPRINT = "9C48ED4A1B413005B7FCF3C6340670DF38D20146"
ARTIFACTS = ("contract-java", "contract-core", "contract-spring-boot-starter")
CENTRAL = "https://repo.maven.apache.org/maven2"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
STABLE = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\Z")


def require(condition: object, message: str) -> None:
    if not condition:
        raise ValueError(message)


def run(*args: str, **kwargs: Any) -> str:
    return subprocess.check_output(args, text=True, **kwargs).strip()


def git(*args: str) -> str:
    return run("git", *args)


def gh(path: str, method: str = "GET", data: JsonObject | None = None) -> Any:
    args = ["gh", "api", "--method", method, path]
    if data is not None:
        args += ["--input", "-"]
    output = run(*args, input=json.dumps(data) if data is not None else None)
    return json.loads(output) if output else None


def fetch(
    url: str,
    headers: dict[str, str] | None = None,
    data: bytes | None = None,
    method: str | None = None,
    missing: bool = False,
) -> bytes | None:
    request = urllib.request.Request(
        url, headers=headers or {}, data=data, method=method
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        if missing and error.code == 404:
            return None
        # Never echo headers, request bodies, or authentication responses.
        raise ValueError(f"HTTP {error.code} for {url.split('?')[0]}") from None


def digest(path: str | Path, algorithm: str = "sha256") -> str:
    return hashlib.new(algorithm, Path(path).read_bytes()).hexdigest()


def write_json(path: str | Path, value: JsonObject) -> None:
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(
        json.dumps(value, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )


def version_tuple(version: str) -> tuple[int, ...]:
    require(STABLE.fullmatch(version), "Expected a canonical stable X.Y.Z version")
    return tuple(map(int, version.split(".")))


def revision(xml: str) -> str | None:
    return ET.fromstring(xml).findtext("m:properties/m:revision", namespaces=NS)


def timestamp(sha: str) -> tuple[int, str]:
    epoch = int(git("show", "-s", "--format=%ct", sha))
    return epoch, dt.datetime.fromtimestamp(epoch, dt.timezone.utc).isoformat().replace(
        "+00:00", "Z"
    )


def verify_signature(path: str | Path, fingerprint: str = FINGERPRINT) -> None:
    output = run(
        "gpg",
        "--batch",
        "--status-fd",
        "1",
        "--verify",
        str(path) + ".asc",
        str(path),
        stderr=subprocess.DEVNULL,
    )
    valid = [
        line.split()
        for line in output.splitlines()
        if line.startswith("[GNUPG:] VALIDSIG ")
    ]
    require(
        len(valid) == 1 and fingerprint in (valid[0][2], valid[0][-1]),
        "Unexpected signature issuer",
    )


def maven_path(artifact: str, version: str) -> str:
    return f"media/barney/{artifact}/{version}/{artifact}-{version}"


def candidate() -> Release:
    path = Path(os.environ.get("RELEASE_CANDIDATE", "target/release-candidate.json"))
    if not path.exists():
        path = Path("target/release-payload/release-candidate.json")
    value = json.loads(path.read_text(encoding="utf-8"))
    require(re.fullmatch(r"[0-9a-f]{40}", value["sha"]), "Invalid candidate source SHA")
    version_tuple(value["version"])
    return cast(Release, value)


def output(values: dict[str, str | int]) -> None:
    destination = os.environ.get("GITHUB_OUTPUT")
    if destination:
        with open(destination, "a", encoding="utf-8") as stream:
            for key, value in values.items():
                stream.write(f"{key}={value}\n")
