"""Regression tests for release selection, fail-closed gates, and encrypted signing."""

import contextlib
import datetime as dt
import io
import json
import os
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
from release_common import (
    ARTIFACTS,
    FINGERPRINT,
    digest,
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
        with (
            patch.object(publish, "verify"),
            patch.object(publish, "verify_tag"),
            patch.object(publish, "central_downloads"),
            patch.object(publish, "github_assets"),
            patch.object(publish, "attestations"),
            patch.object(publish, "fetch", return_value=page),
            patch.object(publish, "release_record", return_value=self.record),
            patch.object(publish, "gh", side_effect=self.metadata_api),
        ):
            publish.finalize(self.release)
        self.assertFalse(self.record["draft"])
        self.assertEqual(self.record["tag_name"], "v1.0.0")
        self.assertEqual(self.record["target_commitish"], "main")
        self.assertEqual(self.record["make_latest"], "true")
        self.assertEqual(self.record["assets"], [{"id": 11, "name": "retained.jar"}])


class PayloadTests(unittest.TestCase):
    def test_main_jars_require_java_21_bytecode(self):
        valid = b"\xca\xfe\xba\xbe\x00\x00\x00\x41"
        cases = (
            (valid, None),
            (valid[:-1] + b"\x3d", "Expected Java 21 bytecode"),
            (valid[:-1] + b"\x45", "Expected Java 21 bytecode"),
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


class EncryptedSigningTests(unittest.TestCase):
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
        with self.credentials(self.password) as temp:
            payload.import_key(self.fingerprint)
            root = temp / "payload"
            assets = root / "github"
            assets.mkdir(parents=True)
            for artifact in ARTIFACTS:
                prefix = root / "central" / maven_path(artifact, "1.0.0")
                prefix.parent.mkdir(parents=True)
                extensions = [".pom", "-cyclonedx.json", "-cyclonedx.xml"]
                if artifact != "contract-java":
                    extensions += [".jar", "-sources.jar", "-javadoc.jar"]
                    (assets / f"{artifact}-1.0.0.jar").write_bytes(b"jar fixture")
                for extension in extensions:
                    Path(str(prefix) + extension).write_bytes(b"payload fixture")
                (assets / f"{artifact}-1.0.0.cdx.json").write_bytes(b"sbom fixture")
            for algorithm in ("sha256", "sha512"):
                lines = "".join(
                    f"{digest(path, algorithm)}  {path.name}\n"
                    for path in sorted(assets.iterdir())
                    if path.suffix in (".jar", ".json")
                )
                (assets / (algorithm.upper() + "SUMS")).write_text(lines)
            now = int(dt.datetime.now(dt.timezone.utc).timestamp())
            release = {
                "version": "1.0.0",
                "sha": "a" * 40,
                "epoch": now,
                "timestamp": dt.datetime.fromtimestamp(
                    now, dt.timezone.utc
                ).isoformat(),
            }
            with (
                patch.object(payload, "verify_pom"),
                patch.object(payload, "verify_bom"),
                patch.object(payload, "verify_jar"),
            ):
                payload.seal(release, root, self.fingerprint)
                self.assertEqual(len(list(assets.iterdir())), 14)
                self.assertEqual(
                    len([p for p in (root / "central").rglob("*") if p.is_file()]), 150
                )
                (assets / "contract-core-1.0.0.jar").write_bytes(b"tampered")
                with self.assertRaisesRegex(ValueError, "digest mismatch"):
                    payload.verify(root, release, self.fingerprint)


if __name__ == "__main__":
    unittest.main()
