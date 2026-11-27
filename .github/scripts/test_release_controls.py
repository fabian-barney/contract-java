"""Regression tests for release selection, fail-closed gates, signing, and recovery."""

import contextlib
import datetime as dt
import io
import json
import os
import shutil
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

import release_consumer as consumer
import release_control as control
import release_payload as payload
import release_publish as publish
import release_restore as vault
from release_common import (
    ARTIFACTS,
    FINGERPRINT,
    REPOSITORY,
    configured_java_release,
    digest,
    java_release,
    maven_path,
    version_tuple,
)


class CandidateTests(unittest.TestCase):
    def test_candidate_collisions_and_version_floor(self):
        sha = "a" * 40
        parent = "b" * 40
        pom = '<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><revision>{}</revision></properties></project>'
        published = {"draft": False, "prerelease": False, "tag_name": "v0.1.14"}

        def git_result(*args):
            if args == ("rev-parse", "HEAD"):
                return sha
            if args == ("rev-parse", f"{sha}^1"):
                return parent
            if args == ("show", f"{sha}:pom.xml"):
                return pom.format("1.0.0")
            if args == ("show", f"{parent}:pom.xml"):
                return pom.format("0.1.13-SNAPSHOT")
            if args == ("show", f"{sha}:CHANGELOG.md"):
                return "## 1.0.0 - 2026-01-01\n\n- Stable.\n"
            if args[0] == "ls-remote":
                return ""
            raise AssertionError(args)

        conflicting = {
            "draft": True,
            "prerelease": False,
            "tag_name": "v1.0.0",
            "body": "source: another commit",
        }
        for releases, central in (
            ([published], b"existing POM"),
            ([published, conflicting], None),
            ([dict(published, tag_name="v1.0.0")], None),
        ):

            def github_result(path):
                return (
                    {"protected": True} if path.endswith("branches/main") else releases
                )

            with (
                patch.object(control, "git", side_effect=git_result),
                patch.object(control, "gh", side_effect=github_result),
                patch.object(control, "fetch", return_value=central),
                self.assertRaises(ValueError),
            ):
                control.select(sha)

    def test_noop_and_snapshot_transitions(self):
        for old, new in (
            ("1.0.0", "1.0.0"),
            ("0.9.0-SNAPSHOT", "1.0.0-SNAPSHOT"),
            ("1.0.0", "1.0.1-SNAPSHOT"),
        ):
            self.assertFalse(control.transition(old, new))
        self.assertTrue(control.transition("0.1.13-SNAPSHOT", "1.0.0"))

    def test_invalid_revisions_fail(self):
        for value in (
            None,
            "01.0.0",
            "1.0",
            "1.0.0-rc1",
            "x-SNAPSHOT",
            "1.0.0;echo secret",
        ):
            with self.subTest(value=value), self.assertRaises(ValueError):
                control.transition("0.1.0", value)

    def test_dates_and_notes(self):
        valid = "## 1.0.0 - 2026-01-01\n\n- Stable API.\n\n## 0.1.0 - 2025-01-01\nOld\n"
        self.assertNotIn("Old", control.release_notes(valid, "1.0.0"))
        for invalid in (
            "## 1.0.0 - TBD\nNotes",
            "## 1.0.0 - 2026-02-30\nNotes",
            "## 1.0.0 - 2099-01-01\nNotes",
            "## 1.0.0 - 2026-01-01\n",
        ):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                control.release_notes(invalid, "1.0.0")

    def test_semantic_version_order(self):
        self.assertGreater(version_tuple("1.0.0"), version_tuple("0.1.14"))
        self.assertGreater(version_tuple("1.10.0"), version_tuple("1.9.99"))

    def test_exact_ci_gate(self):
        jobs = [{"name": "verify / required", "conclusion": "success"}]
        self.assertTrue(
            control.required_state(
                {"status": "completed", "conclusion": "success"}, jobs
            )
        )
        self.assertFalse(control.required_state({"status": "in_progress"}, []))
        for conclusion in (
            "failure",
            "cancelled",
            "timed_out",
            "skipped",
            "neutral",
            "action_required",
        ):
            with self.subTest(conclusion=conclusion), self.assertRaises(ValueError):
                control.required_state(
                    {"status": "completed", "conclusion": conclusion}, jobs
                )
        with self.assertRaises(ValueError):
            control.required_state({"status": "completed", "conclusion": "success"}, [])

    def test_wait_times_out_instead_of_accepting_missing_ci(self):
        with (
            patch.object(control.time, "monotonic", side_effect=[0, 10]),
            self.assertRaises(ValueError),
        ):
            control.wait_required("a" * 40, timeout=1)

    def test_lightweight_and_wrong_commit_tags_fail(self):
        with (
            patch.object(control, "git", return_value="commit"),
            self.assertRaises(ValueError),
        ):
            control.verify_tag("v1.0.0", "a" * 40)
        with (
            patch.object(control, "git", side_effect=["tag", "b" * 40]),
            self.assertRaises(ValueError),
        ):
            control.verify_tag("v1.0.0", "a" * 40)

    def test_wrong_tag_fingerprint_fails(self):
        checked = subprocess.CompletedProcess(
            [],
            0,
            "",
            "[GNUPG:] VALIDSIG " + "B" * 40 + " 2026 1 0 4 0 1 10 00 " + "B" * 40,
        )
        with (
            patch.object(control, "git", side_effect=["tag", "a" * 40]),
            patch.object(control.subprocess, "run", return_value=checked),
            self.assertRaises(ValueError),
        ):
            control.verify_tag("v1.0.0", "a" * 40)

    def test_central_conflicting_bytes_fail(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "central").mkdir()
            (root / "central/x.pom").write_bytes(b"expected")
            with (
                patch.object(publish, "ROOT", root),
                patch.object(publish, "fetch", return_value=b"different"),
                self.assertRaises(ValueError),
            ):
                publish.central_downloads(required=False)

    def test_unknown_upload_outcome_prevents_second_upload(self):
        release = {"version": "1.0.0"}
        record = {"draft": True, "body": "<!-- central-upload-started -->"}
        with (
            patch.object(publish, "central_downloads", return_value=(False, False)),
            patch.object(publish, "release_record", return_value=record),
            self.assertRaises(ValueError),
        ):
            publish.publish_central(release)


class PublicationIdentityTests(unittest.TestCase):
    def setUp(self):
        self.release = {"version": "1.0.0", "tag": "v1.0.0", "sha": "a" * 40}
        self.record = {
            "id": 7,
            "tag_name": "v1.0.0",
            "target_commitish": self.release["sha"],
            "draft": True,
            "body": f"<!-- source: {self.release['sha']} -->",
            "assets": [{"id": 11, "name": "retained.jar"}],
        }

    def metadata_api(self, path, method, data):
        self.assertTrue(path.endswith("/releases/7"))
        self.assertEqual(method, "PATCH")
        # Model the observed draft API behavior when a PATCH omits tag_name.
        self.record["tag_name"] = "untagged-fixture"
        self.record.update(data)
        # Existing tags make target_commitish non-authoritative in the REST API.
        self.record["target_commitish"] = "main"
        return dict(self.record)

    def test_central_checkpoints_preserve_the_original_release(self):
        deployment = "da8d9bbf-edec-4426-9729-f7b1f97e0cac"
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "central-bundle.zip").write_bytes(b"original bundle")
            with (
                patch.object(publish, "ROOT", root),
                patch.object(publish, "gh", side_effect=self.metadata_api) as api,
                patch.object(publish, "portal_headers", return_value={}),
                patch.object(publish, "fetch", return_value=deployment.encode()),
            ):
                self.assertEqual(
                    publish.upload_bundle(self.record, self.release), deployment
                )
            self.assertEqual(api.call_count, 2)
            self.assertEqual(self.record["tag_name"], "v1.0.0")
            self.assertEqual(self.record["target_commitish"], "main")
            self.assertTrue(self.record["draft"])
            self.assertIn("central-upload-started", self.record["body"])
            self.assertIn(f"central-deployment: {deployment}", self.record["body"])
            self.assertEqual(
                self.record["assets"], [{"id": 11, "name": "retained.jar"}]
            )

    def test_identity_drift_stops_before_central_upload(self):
        for change in (
            {"id": 8},
            {"tag_name": "untagged-fixture"},
            {"body": "source: another commit"},
        ):
            changed = dict(self.record, **change)
            with (
                self.subTest(change=change),
                patch.object(publish, "gh", return_value=changed),
                patch.object(publish, "fetch") as upload,
                self.assertRaisesRegex(ValueError, "Release identity changed"),
            ):
                publish.upload_bundle(self.record, self.release)
            upload.assert_not_called()

    def test_finalization_preserves_the_original_release(self):
        page = f'<html data-source-sha="{self.release["sha"]}"></html>'.encode()
        discarded = []
        with (
            patch.object(publish, "verify"),
            patch.object(publish, "verify_tag"),
            patch.object(publish, "central_downloads"),
            patch.object(publish, "github_assets"),
            patch.object(publish, "attestations"),
            patch.object(publish, "fetch", return_value=page),
            patch.object(publish, "release_record", return_value=self.record),
            patch.object(publish, "gh", side_effect=self.metadata_api),
            patch.object(
                publish,
                "discard",
                side_effect=lambda release: discarded.append(self.record["draft"]),
            ),
        ):
            publish.finalize(self.release)
        # The vault is deleted only once the release is public.
        self.assertEqual(discarded, [False])
        self.assertFalse(self.record["draft"])
        self.assertEqual(self.record["tag_name"], "v1.0.0")
        self.assertEqual(self.record["target_commitish"], "main")
        self.assertEqual(self.record["make_latest"], "true")
        self.assertEqual(self.record["assets"], [{"id": 11, "name": "retained.jar"}])


    def test_finalization_keeps_the_vault_while_the_release_is_a_draft(self):
        page = f'<html data-source-sha="{self.release["sha"]}"></html>'.encode()

        def still_draft(path, method, data):
            return dict(self.metadata_api(path, method, data), draft=True)

        with (
            patch.object(publish, "verify"),
            patch.object(publish, "verify_tag"),
            patch.object(publish, "central_downloads"),
            patch.object(publish, "github_assets"),
            patch.object(publish, "attestations"),
            patch.object(publish, "fetch", return_value=page),
            patch.object(publish, "release_record", return_value=self.record),
            patch.object(publish, "gh", side_effect=still_draft),
            patch.object(publish, "discard") as discard,
            self.assertRaisesRegex(ValueError, "still a draft"),
        ):
            publish.finalize(self.release)
        discard.assert_not_called()


class PayloadTests(unittest.TestCase):
    def test_java_release_reads_configured_bytecode_target(self):
        pom = (
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<properties>{}</properties></project>"
        )
        self.assertEqual(
            java_release(
                pom.format("<maven.compiler.release>25</maven.compiler.release>")
            ),
            25,
        )
        for properties in (
            "",
            "<maven.compiler.release></maven.compiler.release>",
            "<maven.compiler.release>twenty-five</maven.compiler.release>",
            "<maven.compiler.release>${java.version}</maven.compiler.release>",
        ):
            with (
                self.subTest(properties=properties),
                self.assertRaisesRegex(ValueError, "maven.compiler.release"),
            ):
                java_release(pom.format(properties))

    def test_main_jars_require_configured_bytecode(self):
        release = configured_java_release()
        major = release + 44
        valid = b"\xca\xfe\xba\xbe\x00\x00" + major.to_bytes(2, "big")
        message = f"Expected Java {release} bytecode"
        cases = (
            (valid, None),
            (valid[:-2] + (major - 1).to_bytes(2, "big"), message),
            (valid[:-2] + (major + 1).to_bytes(2, "big"), message),
            (b"invalid!", "Malformed class header"),
            (b"\xca\xfe", "Malformed class header"),
        )
        with tempfile.TemporaryDirectory() as temp:
            jar = Path(temp) / "contract-core-1.0.0.jar"
            for class_name in ("module-info.class", "media/barney/contract/Contract.class"):
                for header, error in cases:
                    with self.subTest(class_name=class_name, header=header):
                        entries = {
                            "module-info.class": valid,
                            "media/barney/contract/Contract.class": valid,
                            "META-INF/services/javax.annotation.processing.Processor": b"ContractProcessor",
                        }
                        entries[class_name] = header
                        with zipfile.ZipFile(jar, "w") as archive:
                            for name in payload.LEGAL:
                                archive.writestr("META-INF/" + name, Path(name).read_bytes())
                            for name, data in entries.items():
                                archive.writestr(name, data)
                        if error:
                            with self.assertRaisesRegex(ValueError, error):
                                payload.verify_jar(jar, main=True)
                        else:
                            payload.verify_jar(jar, main=True)

    def test_consumers_follow_resolved_annotation_versions(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            pom = root / "core.pom"
            pom.write_text(
                '<project xmlns="http://maven.apache.org/POM/4.0.0"><dependencies>'
                "<dependency><groupId>org.apiguardian</groupId><artifactId>apiguardian-api</artifactId><version>2.0.0</version></dependency>"
                "<dependency><groupId>org.jspecify</groupId><artifactId>jspecify</artifactId><version>3.0.0</version></dependency>"
                "</dependencies></project>"
            )
            paths = consumer.annotation_paths(pom, root)
            self.assertEqual(
                [path.relative_to(root).as_posix() for path in paths],
                [
                    "org/apiguardian/apiguardian-api/2.0.0/apiguardian-api-2.0.0.jar",
                    "org/jspecify/jspecify/3.0.0/jspecify-3.0.0.jar",
                ],
            )
            pom.write_text(pom.read_text().replace("3.0.0", "${jspecify.version}"))
            with self.assertRaisesRegex(ValueError, "Unresolved consumer dependency"):
                consumer.annotation_paths(pom, root)

    def test_consumers_use_only_current_release_artifacts(self):
        commands = []
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            current = root / "contract-core/target/contract-core-3.0.0.jar"
            current.parent.mkdir(parents=True)
            current.write_bytes(b"current")
            annotations = [root / "apiguardian-api.jar", root / "jspecify.jar"]
            for path in annotations:
                path.write_bytes(b"annotations")
            directory = os.getcwd()
            os.chdir(root)
            try:
                with (
                    patch.object(consumer, "annotation_paths", return_value=annotations),
                    patch.object(consumer, "configured_java_release", return_value=25),
                    patch.object(
                        consumer.subprocess,
                        "run",
                        side_effect=lambda command, check: commands.append(command),
                    ),
                    contextlib.redirect_stdout(io.StringIO()),
                ):
                    consumer.consumers("3.0.0")
            finally:
                os.chdir(directory)
            allowed = {str(current.resolve()), *map(str, annotations)}
        self.assertEqual(
            [command[0] for command in commands], ["javac", "java", "javac", "java"]
        )
        jars = {
            entry
            for command in commands
            for argument in command
            for entry in argument.split(os.pathsep)
            if entry.endswith(".jar")
        }
        self.assertEqual(jars, allowed)

    def test_unresolved_pom_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "artifact.pom"
            path.write_text(
                '<project xmlns="http://maven.apache.org/POM/4.0.0"><version>${revision}</version></project>'
            )
            with self.assertRaises(ValueError):
                payload.verify_pom(path, "contract-core", "1.0.0")

    def test_broken_graph_and_test_dependencies_fail(self):
        bom = {
            "metadata": {
                "component": {
                    "purl": "pkg:maven/media.barney/contract-core@1.0.0?type=jar",
                    "bom-ref": "root",
                }
            },
            "dependencies": [{"ref": "root", "dependsOn": ["missing"]}],
        }
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "bom.json"
            path.write_text(json.dumps(bom))
            with self.assertRaises(ValueError):
                payload.verify_bom(path, "contract-core", "1.0.0")
            bom["dependencies"] = []
            bom["components"] = [
                {
                    "bom-ref": "test",
                    "group": "org.junit.jupiter",
                    "name": "junit-jupiter",
                }
            ]
            path.write_text(json.dumps(bom))
            with self.assertRaises(ValueError):
                payload.verify_bom(path, "contract-core", "1.0.0")

    def test_normalization_preserves_optional_dependency(self):
        identity = "pkg:maven/media.barney/contract-core@1.0.0?type=jar"
        dependency = "pkg:maven/org.example/optional@1.0.0?type=jar"
        bom = {
            "metadata": {"component": {"purl": identity, "bom-ref": identity}},
            "components": [{"purl": dependency, "bom-ref": dependency}],
        }
        xml = (
            '<bom xmlns="http://cyclonedx.org/schema/bom/1.6"><metadata/><components><component><purl>'
            + dependency
            + "</purl></component></components></bom>"
        )
        pom = '<project xmlns="http://maven.apache.org/POM/4.0.0"><dependencies><dependency><groupId>org.example</groupId><artifactId>optional</artifactId><version>1.0.0</version><optional>true</optional></dependency></dependencies></project>'
        with tempfile.TemporaryDirectory() as temp:
            json_path, xml_path = Path(temp) / "bom.json", Path(temp) / "bom.xml"
            json_path.write_text(json.dumps(bom))
            xml_path.write_text(xml)
            release = {
                "version": "1.0.0",
                "sha": "a" * 40,
                "timestamp": "2026-01-01T00:00:00Z",
            }
            payload.normalize_bom(json_path, xml_path, "contract-core", release, pom)
            first = json_path.read_bytes(), xml_path.read_bytes()
            result = json.loads(first[0])
            self.assertEqual(result["components"][0]["properties"][0]["value"], "true")
            self.assertIn(b"timestamp", first[1])


class SigningKeyCase(unittest.TestCase):
    """Provide an ephemeral encrypted signing key and sealed fixture payloads."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="contract-gpg-test-")
        cls.keyring = Path(cls.temp.name) / "origin"
        cls.keyring.mkdir(mode=0o700)
        cls.password = "ephemeral-test-passphrase"
        env = dict(os.environ, GNUPGHOME=str(cls.keyring))
        result = subprocess.run(
            [
                "gpg",
                "--batch",
                "--pinentry-mode",
                "loopback",
                "--passphrase-fd",
                "0",
                "--quick-generate-key",
                "Release Test <release@example.invalid>",
                "rsa2048",
                "sign",
                "0",
            ],
            input=(cls.password + "\n").encode(),
            env=env,
            capture_output=True,
        )
        if result.returncode:
            raise RuntimeError("Ephemeral test key generation failed")
        listing = subprocess.check_output(
            ["gpg", "--batch", "--with-colons", "--list-secret-keys"],
            env=env,
            text=True,
        )
        cls.fingerprint = next(
            line.split(":")[9]
            for line in listing.splitlines()
            if line.startswith("fpr:")
        )
        cls.key = subprocess.run(
            [
                "gpg",
                "--batch",
                "--pinentry-mode",
                "loopback",
                "--passphrase-fd",
                "0",
                "--armor",
                "--export-secret-keys",
                cls.fingerprint,
            ],
            input=(cls.password + "\n").encode(),
            env=env,
            capture_output=True,
            check=True,
        ).stdout.decode()

    @classmethod
    def tearDownClass(cls):
        subprocess.run(
            ["gpgconf", "--kill", "gpg-agent"],
            env=dict(os.environ, GNUPGHOME=str(cls.keyring)),
            capture_output=True,
        )
        cls.temp.cleanup()

    @contextlib.contextmanager
    def credentials(self, password):
        with tempfile.TemporaryDirectory(prefix="contract-gpg-import-") as temp:
            env = {
                "GNUPGHOME": temp,
                "MAVEN_GPG_PRIVATE_KEY": self.key,
                "MAVEN_GPG_PASSPHRASE": password,
            }
            with patch.dict(os.environ, env):
                try:
                    yield Path(temp)
                finally:
                    subprocess.run(
                        ["gpgconf", "--kill", "gpg-agent"], capture_output=True
                    )

    @contextlib.contextmanager
    def fixture_payloads(self):
        # Fixture bytes are not real POMs, SBOMs, or JARs; signatures and digests are.
        with (
            patch.object(payload, "verify_pom"),
            patch.object(payload, "verify_bom"),
            patch.object(payload, "verify_jar"),
        ):
            yield

    @staticmethod
    def fixture_release(retry=False):
        now = int(dt.datetime.now(dt.timezone.utc).timestamp())
        return {
            "version": "1.0.0",
            "sha": "a" * 40,
            "epoch": now,
            "timestamp": dt.datetime.fromtimestamp(now, dt.timezone.utc).isoformat(),
            "tag": "v1.0.0",
            "notes": "## 1.0.0 - 2026-01-01\n",
            "retry": retry,
        }

    def seal_fixture(self, root, release):
        """Stage and seal a fixture payload the way the release build does."""
        assets = root / "github"
        assets.mkdir(parents=True)
        for artifact in ARTIFACTS:
            prefix = root / "central" / maven_path(artifact, release["version"])
            prefix.parent.mkdir(parents=True)
            extensions = [".pom", "-cyclonedx.json", "-cyclonedx.xml"]
            if artifact != "contract-java":
                extensions += [".jar", "-sources.jar", "-javadoc.jar"]
                (assets / f"{artifact}-{release['version']}.jar").write_bytes(
                    b"jar fixture"
                )
            for extension in extensions:
                Path(str(prefix) + extension).write_bytes(b"payload fixture")
            (assets / f"{artifact}-{release['version']}.cdx.json").write_bytes(
                b"sbom fixture"
            )
        for algorithm in ("sha256", "sha512"):
            lines = "".join(
                f"{digest(path, algorithm)}  {path.name}\n"
                for path in sorted(assets.iterdir())
                if path.suffix in (".jar", ".json")
            )
            (assets / (algorithm.upper() + "SUMS")).write_text(lines)
        payload.seal(release, root, self.fingerprint)


class EncryptedSigningTests(SigningKeyCase):
    def test_correct_credentials_and_modified_file(self):
        with self.credentials(self.password) as temp:
            payload.import_key(self.fingerprint)
            artifact = temp / "artifact.jar"
            artifact.write_bytes(b"original release bytes")
            payload.sign(artifact, expected=self.fingerprint)
            payload.verify_signature(artifact, self.fingerprint)
            artifact.write_bytes(b"modified release bytes")
            with self.assertRaises(subprocess.CalledProcessError):
                payload.verify_signature(artifact, self.fingerprint)

    def test_wrong_and_missing_passphrase_do_not_leak(self):
        for password in ("", "incorrect-secret-test"):
            output = io.StringIO()
            with (
                self.credentials(password),
                contextlib.redirect_stdout(output),
                contextlib.redirect_stderr(output),
            ):
                with self.assertRaises(ValueError) as error:
                    payload.import_key(self.fingerprint)
            self.assertNotIn(
                "incorrect-secret-test", str(error.exception) + output.getvalue()
            )
            self.assertNotIn("PRIVATE KEY", str(error.exception) + output.getvalue())

    def test_fingerprint_mismatch_fails_before_signing(self):
        with (
            self.credentials(self.password),
            self.assertRaisesRegex(ValueError, "fingerprint mismatch"),
        ):
            payload.import_key(FINGERPRINT)

    def test_child_processes_do_not_inherit_publishing_secrets(self):
        with self.credentials(self.password):
            self.assertNotIn("MAVEN_GPG_PASSPHRASE", payload.secret_environment())
            self.assertNotIn("MAVEN_GPG_PRIVATE_KEY", payload.secret_environment())

    def test_sealed_inventory_and_corrupt_recovery(self):
        with self.credentials(self.password) as temp, self.fixture_payloads():
            payload.import_key(self.fingerprint)
            root = temp / "payload"
            release = self.fixture_release()
            self.seal_fixture(root, release)
            assets = root / "github"
            self.assertEqual(len(list(assets.iterdir())), 14)
            self.assertEqual(
                len([p for p in (root / "central").rglob("*") if p.is_file()]), 150
            )
            (assets / "contract-core-1.0.0.jar").write_bytes(b"tampered")
            with self.assertRaisesRegex(ValueError, "digest mismatch"):
                payload.verify(root, release, self.fingerprint)


class FakeReleases:
    """In-memory releases API; drafts and their assets survive every rerun.

    With `lagging`, the release list omits writes until `settle()`, as GitHub's
    list did in the #255 dry run right after a draft was created.
    """

    def __init__(self, lagging=False):
        self.records = []
        self.blobs = {}
        self.uploads = 0
        self.ids = iter(range(100, 10_000))
        self.lagging = lagging
        self.unsettled = set()

    def settle(self):
        self.unsettled.clear()

    def api(self, path, method="GET", data=None):
        releases = f"repos/{REPOSITORY}/releases"
        if (method, path) == ("GET", releases + "?per_page=100"):
            listed = [
                dict(
                    record,
                    assets=[
                        asset
                        for asset in record["assets"]
                        if asset["id"] not in self.unsettled
                    ],
                )
                for record in self.records
                if record["id"] not in self.unsettled
            ]
            return json.loads(json.dumps(listed))
        if (method, path) == ("POST", releases):
            self.records.append(dict(data, id=next(self.ids), assets=[]))
            if self.lagging:
                self.unsettled.add(self.records[-1]["id"])
            return json.loads(json.dumps(self.records[-1]))
        if method == "DELETE" and path.startswith(releases + "/assets/"):
            asset = int(path.rsplit("/", 1)[1])
            for record in self.records:
                record["assets"] = [a for a in record["assets"] if a["id"] != asset]
            return None
        if method == "DELETE" and path.startswith(releases + "/"):
            release = int(path.rsplit("/", 1)[1])
            self.records = [r for r in self.records if r["id"] != release]
            return None
        raise AssertionError(f"Unexpected API call: {method} {path}")

    def upload(self, record, archive):
        self.uploads += 1
        asset = {"id": next(self.ids), "name": archive.name, "state": "uploaded"}
        self.blobs[asset["id"]] = archive.read_bytes()
        target = next(r for r in self.records if r["id"] == record["id"])
        target["assets"].append(asset)
        if self.lagging:
            self.unsettled.add(asset["id"])
        return dict(asset)

    def download(self, asset):
        return self.blobs[asset["id"]]

    def tamper(self, name):
        [asset] = self.blobs
        buffer = io.BytesIO()
        with (
            zipfile.ZipFile(io.BytesIO(self.blobs[asset])) as original,
            zipfile.ZipFile(buffer, "w") as changed,
        ):
            for entry in original.infolist():
                data = original.read(entry)
                changed.writestr(entry, b"tampered" if entry.filename == name else data)
        self.blobs[asset] = buffer.getvalue()


def tree(root):
    return {
        path.relative_to(root).as_posix(): path.read_bytes()
        for path in root.rglob("*")
        if path.is_file()
    }


class PayloadVaultTests(SigningKeyCase):
    """Simulate failed publications and reruns against the draft vault."""

    def setUp(self):
        self.github = FakeReleases()
        self.outputs = {}
        stack = contextlib.ExitStack()
        self.addCleanup(stack.close)
        temp = stack.enter_context(self.credentials(self.password))
        payload.import_key(self.fingerprint)
        self.root = temp / "release-payload"
        stack.enter_context(self.fixture_payloads())
        stack.enter_context(contextlib.redirect_stdout(io.StringIO()))
        for name, value in {
            "ROOT": self.root,
            "ARCHIVES": temp / "release-vault",
            "gh": self.github.api,
            "upload": self.github.upload,
            "download": self.github.download,
            "output": self.outputs.update,
            "verify": lambda root, release: payload.verify(
                root, release, self.fingerprint
            ),
        }.items():
            stack.enter_context(patch.object(vault, name, value))
        self.release = self.fixture_release()
        self.sha = self.release["sha"]

    def build_and_retain(self):
        """Run the first attempt up to the tag: no vault yet, so build and seal."""
        vault.restore(self.release)
        self.assertEqual(self.outputs, {"restored": "false", "needs-signing": "true"})
        self.seal_fixture(self.root, self.release)
        sealed = tree(self.root)
        vault.retain(self.release)
        return sealed

    def fresh_runner(self):
        # A rerun starts without the workspace or the earlier attempt's artifacts.
        shutil.rmtree(self.root)
        self.outputs.clear()

    def test_rerun_after_tag_restores_identical_payload_without_rebuilding(self):
        sealed = self.build_and_retain()
        # The signed tag is pushed, then Central publication fails.
        self.fresh_runner()
        rerun = dict(self.release, retry=True)
        vault.restore(rerun)
        # restored=true skips the build and seal steps; RecoveryWorkflowTests
        # checks that gating in release.yml.
        self.assertEqual(self.outputs, {"restored": "true", "needs-signing": "false"})
        self.assertEqual(tree(self.root), sealed)
        vault.retain(rerun)
        self.assertEqual(self.github.uploads, 1)
        [record] = self.github.records
        self.assertEqual(record["tag_name"], "release-payload-" + self.sha)
        self.assertTrue(record["draft"])
        self.assertIn(f"source: {self.sha}", record["body"])
        self.assertEqual(
            [asset["name"] for asset in record["assets"]],
            [f"release-payload-{self.sha}.zip"],
        )

    def test_retain_trusts_write_responses_while_the_release_list_lags(self):
        self.github.lagging = True
        sealed = self.build_and_retain()
        self.assertEqual(self.github.uploads, 1)
        self.assertEqual(len(self.github.records), 1)
        self.github.settle()
        self.fresh_runner()
        vault.restore(dict(self.release, retry=True))
        self.assertEqual(self.outputs, {"restored": "true", "needs-signing": "false"})
        self.assertEqual(tree(self.root), sealed)

    def test_retain_rejects_a_mismatched_create_response(self):
        self.seal_fixture(self.root, self.release)
        create = self.github.api
        for change in (
            {"draft": False},
            {"body": "<!-- source: other -->"},
            {"tag_name": "untagged-fixture"},
        ):

            def api(path, method="GET", data=None, change=change):
                response = create(path, method, data)
                return dict(response, **change) if method == "POST" else response

            self.github.records = []
            with (
                self.subTest(change=change),
                patch.object(vault, "gh", api),
                self.assertRaisesRegex(ValueError, "identity mismatch"),
            ):
                vault.retain(self.release)
        self.assertEqual(self.github.uploads, 0)

    def test_retain_rejects_an_unfinished_or_misnamed_upload(self):
        self.seal_fixture(self.root, self.release)
        upload = self.github.upload
        for change in ({"state": "open"}, {"name": "other.zip"}):

            def changed(record, archive, change=change):
                return dict(upload(record, archive), **change)

            self.github.records = []
            with (
                self.subTest(change=change),
                patch.object(vault, "upload", changed),
                self.assertRaisesRegex(ValueError, "never overwrite"),
            ):
                vault.retain(self.release)

    def test_failure_before_tagging_reuses_the_vault_and_still_signs_the_tag(self):
        sealed = self.build_and_retain()
        self.fresh_runner()
        vault.restore(self.release)
        self.assertEqual(self.outputs, {"restored": "true", "needs-signing": "true"})
        self.assertEqual(tree(self.root), sealed)

    def test_downstream_jobs_retrieve_the_verified_payload(self):
        sealed = self.build_and_retain()
        self.fresh_runner()
        vault.retrieve(self.sha)
        self.assertEqual(tree(self.root), sealed)
        shutil.rmtree(self.root)
        other = "b" * 40
        with self.assertRaisesRegex(ValueError, "vault is missing"):
            vault.retrieve(other)
        # A vault named for another commit must not yield this commit's payload.
        [record] = self.github.records
        record["tag_name"] = "release-payload-" + other
        record["body"] = f"<!-- source: {other} -->"
        record["assets"][0]["name"] = f"release-payload-{other}.zip"
        with self.assertRaisesRegex(ValueError, "source mismatch"):
            vault.retrieve(other)

    def test_tag_without_vault_is_a_hard_stop(self):
        with self.assertRaisesRegex(ValueError, "do not rebuild"):
            vault.restore(dict(self.release, retry=True))
        self.assertEqual(self.outputs, {})

    def test_tampered_vault_fails_verification(self):
        self.build_and_retain()
        self.github.tamper("github/contract-core-1.0.0.jar")
        self.fresh_runner()
        with self.assertRaisesRegex(ValueError, "digest mismatch"):
            vault.restore(dict(self.release, retry=True))
        self.assertEqual(self.outputs, {})

    def test_conflicting_vault_is_never_overwritten(self):
        self.build_and_retain()
        self.github.tamper("release-candidate.json")
        with self.assertRaisesRegex(ValueError, "never overwrite"):
            vault.retain(self.release)
        self.assertEqual(self.github.uploads, 1)

    def test_interrupted_upload_is_replaced_only_before_tagging(self):
        self.github.api(
            f"repos/{REPOSITORY}/releases",
            "POST",
            {
                "tag_name": "release-payload-" + self.sha,
                "draft": True,
                "body": f"<!-- source: {self.sha} -->",
            },
        )
        partial = {"id": 1, "name": f"release-payload-{self.sha}.zip"}
        self.github.records[0]["assets"].append(dict(partial, state="starter"))
        self.github.blobs[partial["id"]] = b"truncated upload"
        with self.assertRaisesRegex(ValueError, "do not rebuild"):
            vault.restore(dict(self.release, retry=True))
        vault.restore(self.release)
        self.assertEqual(self.outputs, {"restored": "false", "needs-signing": "true"})
        self.seal_fixture(self.root, self.release)
        with self.assertRaisesRegex(ValueError, "Incomplete payload vault"):
            vault.retain(dict(self.release, retry=True))
        vault.retain(self.release)
        [asset] = self.github.records[0]["assets"]
        self.assertEqual(asset["state"], "uploaded")
        self.assertNotEqual(asset["id"], partial["id"])

    def test_vault_identity_is_bound_to_the_source(self):
        name = "release-payload-" + self.sha
        source = f"<!-- source: {self.sha} -->"
        for record in (
            {"tag_name": name, "draft": True, "body": "<!-- source: other -->"},
            {"tag_name": name, "draft": False, "body": source},
        ):
            self.github.records = [dict(record, id=1, assets=[])]
            with (
                self.subTest(record=record),
                self.assertRaisesRegex(ValueError, "identity mismatch"),
            ):
                vault.restore(self.release)
        self.github.records = [
            {"tag_name": name, "draft": True, "body": source, "id": 1, "assets": []},
            {"tag_name": name, "draft": True, "body": source, "id": 2, "assets": []},
        ]
        with self.assertRaisesRegex(ValueError, "Multiple payload vaults"):
            vault.restore(self.release)

    def test_unsafe_vault_entries_are_rejected(self):
        for name in ("../escape", "/absolute", "nested/../../escape"):
            buffer = io.BytesIO()
            with zipfile.ZipFile(buffer, "w") as archive:
                archive.writestr(name, b"payload")
            with (
                self.subTest(name=name),
                self.assertRaisesRegex(ValueError, "Unsafe payload vault entry"),
            ):
                vault.unpack(buffer.getvalue())
            shutil.rmtree(self.root, ignore_errors=True)
        self.assertFalse((self.root.parent / "escape").exists())

    def test_discard_deletes_only_the_vault(self):
        self.build_and_retain()
        public = {"tag_name": "v1.0.0", "draft": False, "id": 7, "assets": []}
        self.github.records.append(public)
        vault.discard(self.release)
        self.assertEqual(self.github.records, [public])
        vault.discard(self.release)
        self.assertEqual(self.github.records, [public])


class RecoveryWorkflowTests(unittest.TestCase):
    @staticmethod
    def step(workflow, name):
        start = workflow.index("- name: " + name)
        return workflow[start:].split("\n      - ", 1)[0]

    def test_vault_precedes_the_tag_and_restoring_skips_the_build(self):
        workflows = Path(".github/workflows")
        release = (workflows / "release.yml").read_text(encoding="utf-8")
        order = [
            "Restore sealed payload from its draft vault",
            "Build unsigned release payload once",
            "Sign and validate retained bytes",
            "Retain sealed payload in a draft vault before tagging",
            "Create, verify, and push signed annotated tag",
            "Publish prebuilt Central bundle and verify downloads",
        ]
        positions = [release.index("- name: " + name) for name in order]
        self.assertEqual(positions, sorted(positions))
        for name in order[1:3]:
            self.assertIn(
                "if: steps.restore.outputs.restored != 'true'",
                self.step(release, name),
            )
        self.assertNotIn("if:", self.step(release, order[3]))
        pages = (workflows / "javadoc-pages.yml").read_text(encoding="utf-8")
        for workflow in (release, pages):
            self.assertNotIn("upload-artifact", workflow)
            self.assertNotIn("download-artifact", workflow)
            self.assertIn("release_restore.py retrieve", workflow)


if __name__ == "__main__":
    unittest.main()
