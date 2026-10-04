"""Stage once, normalize before signing, and verify the retained release bytes."""

import argparse
import base64
import json
import os
import shutil
import subprocess
import uuid
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from release_common import (
    ARTIFACTS,
    FINGERPRINT,
    NS,
    Release,
    candidate,
    digest,
    maven_path,
    require,
    run,
    verify_signature,
    write_json,
)

LEGAL = ("LICENSE", "NOTICE", "THIRD-PARTY-LICENSES.md")


def secret_environment() -> dict[str, str]:
    return {
        key: value
        for key, value in os.environ.items()
        if key
        not in (
            "MAVEN_GPG_PRIVATE_KEY",
            "MAVEN_GPG_PASSPHRASE",
            "MAVEN_CENTRAL_TOKEN_USERNAME",
            "MAVEN_CENTRAL_TOKEN_PASSWORD",
        )
    }


def import_key(expected: str = FINGERPRINT) -> None:
    home = os.environ.get("GNUPGHOME")
    require(home and Path(home).is_dir(), "An isolated signing keyring is required")
    key = decode_private_key()
    require(
        key and os.environ.get("MAVEN_GPG_PASSPHRASE"), "Missing signing credentials"
    )
    result = subprocess.run(
        ["gpg", "--batch", "--import"],
        input=key,
        env=secret_environment(),
        capture_output=True,
    )
    require(result.returncode == 0, "Private key import failed")
    keys = run(
        "gpg",
        "--batch",
        "--with-colons",
        "--list-secret-keys",
        env=secret_environment(),
    )
    require(
        primary_fingerprints(keys) == [expected],
        "Private signing key fingerprint mismatch",
    )
    # Prove that the encrypted key can be unlocked before creating a public tag.
    probe = Path(os.environ["GNUPGHOME"]) / "probe"
    probe.write_bytes(b"contract-java signing credential check\n")
    try:
        sign(probe, expected=expected)
        verify_signature(probe, expected)
    finally:
        probe.unlink(missing_ok=True)
        probe.with_suffix(".asc").unlink(missing_ok=True)


def decode_private_key() -> bytes:
    key = os.environ.get("MAVEN_GPG_PRIVATE_KEY", "").encode()
    require(key, "Missing private signing key")
    for _ in range(4):
        if b"BEGIN PGP PRIVATE KEY BLOCK" in key:
            return key
        try:
            key = base64.b64decode(key, validate=True)
        except ValueError:
            raise ValueError("Invalid armored or base64 private key") from None
    raise ValueError("Unsupported private key encoding")


def primary_fingerprints(keys: str) -> list[str]:
    primary = []
    awaiting = False
    for line in keys.splitlines():
        fields = line.split(":")
        if fields[0] == "sec":
            awaiting = True
        elif fields[0] == "fpr" and awaiting:
            primary.append(fields[9])
            awaiting = False
    return primary


def sign(
    path: str | Path, epoch: int | None = None, expected: str = FINGERPRINT
) -> None:
    passphrase = os.environ.get("MAVEN_GPG_PASSPHRASE")
    require(passphrase, "Missing signing passphrase")
    args = [
        "gpg",
        "--batch",
        "--yes",
        "--no-tty",
        "--pinentry-mode",
        "loopback",
        "--passphrase-fd",
        "0",
        "--local-user",
        expected,
        "--armor",
        "--detach-sign",
        "--output",
        str(path) + ".asc",
    ]
    if epoch is not None:
        args += ["--faked-system-time", str(epoch) + "!"]
    result = subprocess.run(
        args + [str(path)],
        input=(passphrase + "\n").encode(),
        env=secret_environment(),
        capture_output=True,
    )
    require(
        result.returncode == 0, "GPG signing failed (credentials or key configuration)"
    )


def pom_dependencies(pom: str) -> list[tuple[str | None, str | None, str | None, str]]:
    root = ET.fromstring(pom)
    return [
        (
            dep.findtext("m:groupId", namespaces=NS),
            dep.findtext("m:artifactId", namespaces=NS),
            dep.findtext("m:version", namespaces=NS),
            dep.findtext("m:optional", "false", NS),
        )
        for dep in root.findall("m:dependencies/m:dependency", NS)
        if dep.findtext("m:scope", "compile", NS) != "test"
    ]


def normalize_bom(
    json_path: Path, xml_path: Path, artifact: str, release: Release, pom: str
) -> None:
    bom = json.loads(json_path.read_text(encoding="utf-8"))
    identity = f"pkg:maven/media.barney/{artifact}@{release['version']}"
    root = bom["metadata"]["component"]
    require(root["purl"].split("?", 1)[0] == identity, "SBOM coordinate mismatch")
    serial = "urn:uuid:" + str(
        uuid.uuid5(uuid.NAMESPACE_URL, f"{identity}:{release['sha']}")
    )
    bom["serialNumber"] = serial
    bom["metadata"]["timestamp"] = release["timestamp"]
    components = {
        item["purl"].split("?", 1)[0]: item for item in bom.get("components", [])
    }
    optional = []
    for group, name, version, flag in pom_dependencies(pom):
        purl = f"pkg:maven/{group}/{name}@{version}"
        require(purl in components, f"SBOM omits production dependency {purl}")
        if flag == "true":
            components[purl].setdefault("properties", []).append(
                {"name": "contract-java:maven:optional", "value": "true"}
            )
            optional.append(purl)
    write_json(json_path, bom)
    tree = ET.parse(xml_path)
    namespace = tree.getroot().tag.partition("}")[0][1:]
    ET.register_namespace("", namespace)
    tree.getroot().set("serialNumber", serial)
    metadata = tree.find(f"{{{namespace}}}metadata")
    date = metadata.find(f"{{{namespace}}}timestamp")
    if date is None:
        date = ET.Element(f"{{{namespace}}}timestamp")
        metadata.insert(0, date)
    date.text = release["timestamp"]
    for component in tree.findall(
        f"{{{namespace}}}components/{{{namespace}}}component"
    ):
        if component.findtext(f"{{{namespace}}}purl", "").split("?", 1)[0] in optional:
            properties = component.find(f"{{{namespace}}}properties")
            if properties is None:
                properties = ET.SubElement(component, f"{{{namespace}}}properties")
            ET.SubElement(
                properties,
                f"{{{namespace}}}property",
                {"name": "contract-java:maven:optional"},
            ).text = "true"
    tree.write(xml_path, encoding="utf-8", xml_declaration=True)


def verify_pom(path: Path, artifact: str, version: str) -> None:
    text = path.read_text(encoding="utf-8")
    require(
        "${" not in text and "-SNAPSHOT" not in text,
        "Unresolved or snapshot publication POM",
    )
    root = ET.fromstring(text)
    require(
        root.findtext("m:artifactId", namespaces=NS) == artifact, "Wrong POM artifact"
    )
    require(root.findtext("m:version", namespaces=NS) == version, "Wrong POM version")
    for _, _, dep_version, _ in pom_dependencies(text):
        require(dep_version, "Unresolved dependency version")


def verify_bom(path: Path, artifact: str, version: str) -> None:
    bom = json.loads(path.read_text(encoding="utf-8"))
    root = bom["metadata"]["component"]
    require(
        root["purl"].split("?", 1)[0] == f"pkg:maven/media.barney/{artifact}@{version}",
        "Wrong SBOM identity",
    )
    refs = {root["bom-ref"]} | {item["bom-ref"] for item in bom.get("components", [])}
    for item in bom.get("dependencies", []):
        require(
            item["ref"] in refs
            and all(ref in refs for ref in item.get("dependsOn", [])),
            "Broken SBOM graph",
        )
    require(
        not any(
            item.get("group", "").startswith(
                ("org.junit", "org.assertj", "org.mockito")
            )
            or item.get("name", "").endswith("-test")
            for item in bom.get("components", [])
        ),
        "Test dependency in SBOM",
    )


def verify_jar(path: Path, main: bool = False) -> None:
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        for name in LEGAL:
            require(
                archive.read("META-INF/" + name) == Path(name).read_bytes(),
                "Missing or incorrect archive legal file",
            )
        if main:
            require("module-info.class" in names, "Missing JPMS descriptor")
            if path.name.startswith("contract-core-"):
                require(
                    b"ContractProcessor"
                    in archive.read(
                        "META-INF/services/javax.annotation.processing.Processor"
                    ),
                    "Missing processor discovery",
                )
            else:
                require(
                    "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
                    in names,
                    "Missing Boot discovery",
                )


def zip_files(destination: Path, files: list[Path], base: Path, epoch: int) -> None:
    import datetime as dt

    date = dt.datetime.fromtimestamp(max(epoch, 315532800), dt.timezone.utc)
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_STORED) as archive:
        for path in sorted(files):
            entry = zipfile.ZipInfo(
                path.relative_to(base).as_posix(), date.timetuple()[:6]
            )
            entry.external_attr = 0o100644 << 16
            archive.writestr(entry, path.read_bytes())


def stage(release: Release, destination: str | Path) -> None:
    root = Path(destination)
    require(not root.exists(), "Refusing to replace staged payload")
    central = root / "central"
    github = root / "github"
    github.mkdir(parents=True)
    for artifact in ARTIFACTS:
        stage_coordinate(artifact, release, central, github)
    payloads = list(github.iterdir())
    require(len(payloads) == 5, "Expected five GitHub payloads")
    for algorithm in ("sha256", "sha512"):
        (github / (algorithm.upper() + "SUMS")).write_text(
            "".join(
                f"{digest(path, algorithm)}  {path.name}\n" for path in sorted(payloads)
            ),
            encoding="utf-8",
        )


def stage_coordinate(
    artifact: str, release: Release, central: Path, github: Path
) -> None:
    module = Path(".") if artifact == "contract-java" else Path(artifact)
    prefix = central / maven_path(artifact, release["version"])
    prefix.parent.mkdir(parents=True, exist_ok=True)
    pom = Path(str(prefix) + ".pom")
    shutil.copyfile(module / ".flattened-pom.xml", pom)
    verify_pom(pom, artifact, release["version"])
    for extension in ("json", "xml"):
        shutil.copyfile(
            module / f"target/bom.{extension}", str(prefix) + f"-cyclonedx.{extension}"
        )
    normalize_bom(
        Path(str(prefix) + "-cyclonedx.json"),
        Path(str(prefix) + "-cyclonedx.xml"),
        artifact,
        release,
        pom.read_text(),
    )
    verify_bom(Path(str(prefix) + "-cyclonedx.json"), artifact, release["version"])
    shutil.copyfile(
        str(prefix) + "-cyclonedx.json",
        github / f"{artifact}-{release['version']}.cdx.json",
    )
    if artifact == "contract-java":
        return
    for classifier in ("", "-sources", "-javadoc"):
        jar = Path(str(prefix) + classifier + ".jar")
        shutil.copyfile(module / "target" / jar.name, jar)
        verify_jar(jar, main=not classifier)
        if not classifier:
            shutil.copyfile(jar, github / jar.name)


def seal(
    release: Release, destination: str | Path, expected: str = FINGERPRINT
) -> None:
    root = Path(destination)
    central = root / "central"
    for path in sorted(central.rglob("*")):
        if path.is_file():
            sign(path, release["epoch"], expected)
            verify_signature(path, expected)
    for path in sorted(central.rglob("*")):
        if path.is_file():
            for algorithm, suffix in (
                ("md5", "md5"),
                ("sha1", "sha1"),
                ("sha256", "sha256"),
                ("sha512", "sha512"),
            ):
                Path(str(path) + "." + suffix).write_text(
                    digest(path, algorithm), encoding="ascii"
                )
    for path in sorted((root / "github").iterdir()):
        sign(path, release["epoch"], expected)
        verify_signature(path, expected)
    zip_files(
        root / "central-bundle.zip",
        [p for p in central.rglob("*") if p.is_file()],
        central,
        release["epoch"],
    )
    write_json(root / "release-candidate.json", release)
    files = {
        path.relative_to(root).as_posix(): digest(path)
        for path in root.rglob("*")
        if path.is_file()
    }
    write_json(
        root / "manifest.json",
        {
            "repository": "fabian-barney/contract-java",
            "sha": release["sha"],
            "version": release["version"],
            "files": files,
        },
    )
    sign(root / "manifest.json", release["epoch"], expected)
    verify(root, release, expected)


def verify(
    root: str | Path, release: Release, expected_fingerprint: str = FINGERPRINT
) -> None:
    root = Path(root)
    verify_signature(root / "manifest.json", expected_fingerprint)
    manifest = json.loads((root / "manifest.json").read_text())
    require(
        manifest["sha"] == release["sha"]
        and manifest["version"] == release["version"]
        and manifest["repository"] == "fabian-barney/contract-java",
        "Retained source mismatch",
    )
    expected = set(manifest["files"]) | {"manifest.json", "manifest.json.asc"}
    require(
        expected
        == {p.relative_to(root).as_posix() for p in root.rglob("*") if p.is_file()},
        "Unexpected retained files",
    )
    for name, checksum in manifest["files"].items():
        path = root / name
        require(
            path.is_file() and digest(path) == checksum,
            "Retained artifact digest mismatch",
        )
        if name.endswith(".asc"):
            verify_signature(Path(str(path)[:-4]), expected_fingerprint)
    verify_central_inventory(root / "central", release)
    verify_github_inventory(root / "github")
    verify_bundle(root)


def coordinate_payloads(central: Path, artifact: str, version: str) -> list[Path]:
    prefix = central / maven_path(artifact, version)
    extensions = [".pom", "-cyclonedx.json", "-cyclonedx.xml"]
    if artifact != "contract-java":
        extensions += [".jar", "-sources.jar", "-javadoc.jar"]
    verify_pom(Path(str(prefix) + ".pom"), artifact, version)
    verify_bom(Path(str(prefix) + "-cyclonedx.json"), artifact, version)
    for extension in extensions:
        if extension.endswith(".jar"):
            verify_jar(Path(str(prefix) + extension), main=extension == ".jar")
    return [Path(str(prefix) + extension) for extension in extensions]


def verify_central_inventory(central: Path, release: Release) -> None:
    bases = [
        path
        for artifact in ARTIFACTS
        for path in coordinate_payloads(central, artifact, release["version"])
    ]
    expected = set()
    for base in bases:
        for signed in (base, Path(str(base) + ".asc")):
            expected.add(signed)
            for algorithm in ("md5", "sha1", "sha256", "sha512"):
                checksum = Path(str(signed) + "." + algorithm)
                expected.add(checksum)
                require(
                    checksum.read_text() == digest(signed, algorithm),
                    "Central checksum mismatch",
                )
    require(
        expected == {path for path in central.rglob("*") if path.is_file()},
        "Central coordinate inventory mismatch",
    )


def verify_github_inventory(github: Path) -> None:
    assets = list(github.iterdir())
    require(len(assets) == 14, "Expected exactly fourteen GitHub assets")
    for algorithm in ("sha256", "sha512"):
        lines = (github / (algorithm.upper() + "SUMS")).read_text().splitlines()
        require(len(lines) == 5, "Expected five checksum payloads")
        for line in lines:
            checksum, name = line.split("  ", 1)
            require(
                Path(name).name == name
                and digest(github / name, algorithm) == checksum,
                "Checksum mismatch",
            )


def verify_bundle(root: Path) -> None:
    with zipfile.ZipFile(root / "central-bundle.zip") as bundle:
        files = {
            p.relative_to(root / "central").as_posix(): p.read_bytes()
            for p in (root / "central").rglob("*")
            if p.is_file()
        }
        require(set(bundle.namelist()) == set(files), "Central bundle entries mismatch")
        require(
            all(bundle.read(name) == data for name, data in files.items()),
            "Central bundle differs from staged bytes",
        )


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("import-key", "stage", "seal", "verify"))
    parser.add_argument("destination", nargs="?", default="target/release-payload")
    args = parser.parse_args()
    if args.command == "import-key":
        import_key()
    elif args.command == "stage":
        stage(candidate(), args.destination)
    elif args.command == "seal":
        seal(candidate(), args.destination)
    else:
        verify(args.destination, candidate())
