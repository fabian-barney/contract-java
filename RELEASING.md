# Releasing contract-java

The parent `media.barney:contract-java`, `contract-core`, and
`contract-spring-boot-starter` publish together to Maven Central and GitHub.
Examples remain unpublished. The 2.x release line requires Java 21 bytecode
and supports full JDK 21/25, Linux/Windows, and Spring Boot 4.0/4.1.
Maven 3.9.16 is the minimum build version. Stable version changes publish
through the reviewed-main workflow described below.

## Support policy

The supported matrix consists of Spring Boot lines, JDK LTS releases, and a
minimum Maven version. The [README compatibility table](README.md#spring-boot)
lists the supported Spring Boot and JDK lines.

- **Spring Boot:** a line is supported from its adoption until it leaves Spring's
  OSS support window, as published in the
  [Spring support policy](https://spring.io/support-policy). Commercial support
  does not count.
- **JDK:** only LTS releases are supported.
  - A JDK LTS reaches end of life when Oracle JDK stops providing free updates
    for it under the NFTC license, as published in
    [Oracle's Java SE support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html).
  - No other source is used. Paid support, such as Oracle Premier or Extended
    Support, never counts, and neither do Oracle's six-month OpenJDK builds on
    jdk.java.net.
  - The bytecode target is the oldest supported LTS.
- **Maven:** the minimum Maven version for building this repository follows
  Apache Maven's end-of-life declarations and the minimum versions required by
  the build plugins.
- **Hard cut:** a line is retired on its end-of-life date, with no grace period.
  Retirement work is prepared ahead of time so it can merge on that date.
  - Retirement removes everything tied to the line: CI matrix entries, version
    properties, profiles and selectors, tests and fixtures, compatibility code,
    workarounds, documentation and table entries, and Dependabot configuration.
- **Adoption:** new Spring Boot minor lines and new JDK LTS releases are adopted
  after their GA release.
  - The newest supported Spring Boot line is the default build line, and it
    defines the versions in the published starter POM.
- **Security fixes:** only the latest release, the newest minor of the newest
  major, receives security fixes. [SECURITY.md](SECURITY.md) describes how to
  report a vulnerability.

## Versioning

Releases follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
The README defines the API compatibility promise. Version bumps follow these
rules:

- **Major:**
  - any removal from the supported matrix: dropping a Spring Boot line or a JDK
    LTS, raising the bytecode target, or raising the Maven minimum
  - any API break

  Retirements that fall due close together may ship in one major.
- **Minor:** adding a supported line, or a new feature.
- **Patch:** fixes, and dependency patch updates that affect published artifacts.
- Changes that only touch tooling, CI, or documentation do not require a release.

## Release trigger

This project-specific policy selects protected-main push candidates and unattended
publication, extending the shared tag/dispatch release defaults. Signed tags are
created inside the validated publication run. Environment restrictions and the
complete required CI gate replace manual deployment approval.

Prepare an issue-linked PR from current remote `main`. Set the root `revision`
to a canonical stable `X.Y.Z` version above every published stable release,
date its changelog section, and update installation examples and API promises.
Finish a fresh review of the latest head, resolve conversations under repository
policy, and require `verify / required` before merging. Repository owner merge
exceptions still require the explicit authorization defined in the AI rules.

The reviewed merge is the trigger. CI compares that push SHA with its first
parent. Unchanged revisions and snapshot transitions succeed without publishing.
The release waits for the complete required CI gate on that exact SHA; subsequent
pushes cannot cancel its verification. Merge one stable version PR at a time and
wait for its complete release before preparing another stable release.

`Release` workflow dispatch runs a nonpublishing preflight on the selected ref.
It tests encrypted signing and release controls, then compares two clean unsigned
builds using the production packaging and normalization path. It has no publishing
secrets and creates no tag or release.

## Repository configuration

- Protect `main`; require `verify / required`, fresh review, and resolved
  conversations without bypassing existing protections.
- The `release` environment permits protected branches and has no manual reviewer.
- Keep `v*` tags immutable against deletion and movement.
- Set Pages to **GitHub Actions**, and allow protected branches in `github-pages`.
- Reuse `MAVEN_GPG_PRIVATE_KEY`, `MAVEN_GPG_PASSPHRASE`,
  `MAVEN_CENTRAL_TOKEN_USERNAME`, and `MAVEN_CENTRAL_TOKEN_PASSWORD`. Only the
  publication job references them. The existing repository secret scope remains;
  changes to trusted workflows therefore require careful review.

The committed public key's pinned primary fingerprint is
`9C48ED4A1B413005B7FCF3C6340670DF38D20146`. An isolated temporary keyring imports
the private key, checks the primary fingerprint, and tests unlocking before any
public tag. Passphrases use environment injection and private standard input/file
descriptors, never command arguments or saved passphrase files. Keyrings are
removed even on failure.

## Payload and publication

The release profile builds flattened resolved POMs, main/sources/Javadoc JARs,
and CycloneDX JSON/XML SBOMs once. Archive timestamps use the source commit time.
Every class in both main JARs, including JPMS descriptors, must use Java 21
class-file major version 65. Release staging rejects malformed or mismatched
class headers before signing or publication.
SBOM timestamps and serial identifiers are deterministic; production graphs omit
test dependencies and annotate direct optional dependencies. Every distributed
JAR includes the project license, notice, and third-party inventory.

Before tagging or upload, CI retains a signed manifest, the complete signed Maven
repository layout, its prebuilt Central ZIP, Javadoc archives, and GitHub assets
in the immutable `release-payload-SHA` Actions artifact (90-day retention).
Copy that artifact to durable storage for recovery beyond Actions retention.
Reruns verify its signed source identity and every digest before using its bytes.

The signed annotated `vX.Y.Z` tag points to the candidate SHA. The same workflow
continues after its push: a `GITHUB_TOKEN` tag push does not start another
workflow. A GitHub draft precedes Central publication.

GitHub has exactly fourteen assets:

- Two main library JARs and three `ARTIFACT-VERSION.cdx.json` SBOMs.
- `SHA256SUMS` and `SHA512SUMS`, each covering those five payloads.
- Seven detached ASCII-armored `.asc` signatures, one for each file above.

Build provenance covers every published Maven payload and both GitHub manifests.
Separate CycloneDX attestations bind each library SBOM to its JAR digest and the
parent SBOM to its POM digest. Verification enforces the repository, release
workflow, protected main source ref, source SHA, subject digest, and SBOM content.
Attestations live in GitHub's attestation API, preserving the fourteen-asset layout.

The Portal API uploads the validated ZIP with automatic publication. Its
deployment ID is recorded in the draft's hidden metadata and workflow log. Every
metadata update explicitly preserves the tag and source SHA and checks the
returned release identity before continuing. CI waits for publication and checks
every Central download against the retained bytes. The explicitly called Pages
workflow extracts the retained Javadoc JARs,
preserves the `gh-pages` version archive, deploys through Actions Pages, and
updates `/api/latest/` by semantic version. Finalization repeats artifact,
signature, provenance, SBOM, and versioned Javadoc checks before publishing GitHub.

## Consumer verification

Download the fourteen release assets and the committed public key, then run:

```sh
gpg --import release-signing-key.asc
gpg --fingerprint 9C48ED4A1B413005B7FCF3C6340670DF38D20146
gpg --verify SHA256SUMS.asc SHA256SUMS
gpg --verify SHA512SUMS.asc SHA512SUMS
sha256sum --check SHA256SUMS
sha512sum --check SHA512SUMS
```

Verify each payload's detached signature too. For provenance, substitute the
actual version and release SHA:

```sh
gh attestation verify contract-core-VERSION.jar \
  --repo fabian-barney/contract-java \
  --signer-workflow fabian-barney/contract-java/.github/workflows/release.yml \
  --source-ref refs/heads/main --source-digest RELEASE_SHA \
  --deny-self-hosted-runners
```

Repeat with `--predicate-type https://cyclonedx.org/bom` to verify the bound SBOM.
The release workflow additionally compares the attested SBOM with the downloaded
JSON and checks signatures against the pinned fingerprint.

## Recovery and next development version

Use **Re-run all jobs** on the original release run. Reruns restore only that
run's retained payload, verify it, reuse the exact signed tag and draft, compare
existing assets, and resume the recorded Central deployment. They never overwrite
different uploaded bytes. A tag without available retained bytes is a hard stop.

If publication and Javadoc succeeded and only finalization failed, recover the
original draft metadata and use **Re-run failed jobs** on that same run. If the
draft lost its tag association, restore its expected tag and source SHA on the
existing release record, preserving its ID and assets. Never create a replacement
draft for the same published version.

If upload's outcome is unknown, the draft's `central-upload-started` marker blocks
a second upload. Recover the original deployment ID from the Portal or recorded
log, add `<!-- central-deployment: UUID -->` to that draft, then rerun. A failed
Central validation requires inspection; do not automatically create another
deployment or delete the original history.

Before public publication, retry only the same commit and verified payload. After
Central publication, recovery may complete verification, Javadoc, or metadata
using the original bytes. Code or payload changes require a new patch version.
Preserve tags, public artifacts, and the draft as a partial-release record until
recovery completes. Never retarget a tag or silently replace assets.

After the complete public release is verified, open a dedicated `X.Y.(Z+1)-SNAPSHOT`
PR. Its merge publishes nothing. Gradle Plugin Portal and private registries are
not publication targets for this project.
