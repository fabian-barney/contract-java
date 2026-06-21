# Contributing

## Contribution Workflow

Use trunk-based development. Keep `main` protected and releasable, and put each
change on a short-lived issue branch named for the issue and concern, such as
`codex/51-contributing-flow`.

Open one pull request per issue. The PR description should link the issue with a
closing keyword, summarize the bounded change, list the checks run, and call out
any residual risk. Review conversations must be answered and resolved before
merge, and the latest pushed head must have green required checks before it is
merged.

Use Conventional Commit subjects prefixed with the issue id:

```text
42 feat(api): add JSpecify nullness annotations
51 docs: expand contributor flow
```

Prefer the type that matches the user-visible change:

- `feat` for supported API or behavior additions
- `fix` for bug fixes
- `docs` for documentation-only changes
- `test` for test-only changes
- `build` for build, dependency, release, or CI mechanics
- `refactor` for behavior-preserving code reshaping

Keep commits focused. Do not bundle unrelated cleanup, generated output, or
version changes into a feature/fix/docs PR unless the issue explicitly requests
them.

## Sign-Off Policy

This project uses the Developer Certificate of Origin (DCO) instead of a
Contributor License Agreement (CLA). Sign off commits with:

```sh
git commit -s -m "51 docs: expand contributor flow"
```

The sign-off states that you have the right to contribute the change under the
project license. Contributions are accepted under the repository's Apache-2.0
license.

## Local Checks

Run the formatter check and the same verification as the default CI gate:

```sh
./mvnw -B -ntp spotless:check verify
```

Run build and tests without quality gates:

```sh
./mvnw -B -ntp -P!quality-gates-all verify
```

Run quality gates in isolation:

```sh
./mvnw -B -ntp -P!quality-gates-all,quality-gate-crap verify
./mvnw -B -ntp -P!quality-gates-all,quality-gate-cognitive verify
```

Apply the repository formatter before committing Java changes:

```sh
./mvnw -B -ntp spotless:apply
```

Formatting changes should stay in the same PR as the code or docs that require
them. Avoid standalone formatter rewrites unless the issue is explicitly about
formatting.

When using `clean`, do not run multiple Maven builds in parallel in the same
workspace. Parallel `clean` executions can race while deleting `target`
directories.

## Spring Boot Smoke Tests

`contract-spring-boot-starter` runs a Failsafe integration smoke test during
`verify`. It packages the current reactor artifacts, installs them into an
isolated temporary Maven repository, and resolves small dependency projects
against the supported Spring Boot `4.0.x` and `4.1.x` BOMs.

Run only the starter and its dependencies with:

```sh
./mvnw -B -ntp -pl contract-spring-boot-starter -am verify
```

## Annotation Processor Development

The processor uses javac internals to rewrite method bodies. Keep
`.mvn/jvm.config` in sync with any new `com.sun.tools.javac.*` packages used by
processor code.

Compile tests for the processor live in
`contract-core/src/test/java/media/barney/contract/processor`.

## Release Skeleton

Do not include version bumps in ordinary feature, fix, or documentation PRs.
Version changes belong in release or snapshot-bump PRs so the changelog,
artifact metadata, tag, and published artifacts stay auditable as one release
unit.

The reactor version is controlled by the root `pom.xml` `revision` property.
Change it by manually editing that single property; do not use
`versions-maven-plugin set-property` unless a future issue explicitly adds that
workflow. Child modules should keep `${revision}` in their parent declarations.
For a release PR, set `revision` to the release version such as `0.1.0`. After
the release is published, open a dedicated snapshot-bump PR that sets
`revision` to the next snapshot version such as `0.1.1-SNAPSHOT`.

Every release PR must update `CHANGELOG.md`: move completed `Unreleased` notes
into the target version section, add the release date, and leave a fresh
`Unreleased` section for the next development cycle. Review the changelog before
tagging or publishing a release. The release workflow rejects target version
sections that still use the `TBD` date marker.

The `release` Maven profile attaches source and Javadoc jars, generates
CycloneDX XML and JSON SBOMs for published modules, signs artifacts with GPG,
flattens published POMs, and configures Sonatype Central Portal publishing. For
local release verification, make sure `gpg` is installed, a release signing key
is available, and `MAVEN_GPG_PASSPHRASE` is set when the key requires a
passphrase:

```sh
./mvnw -B -ntp -Prelease -Drevision=0.1.0 verify
```

In PowerShell, quote dotted `-D` properties:

```powershell
.\mvnw.cmd -B -ntp -Prelease "-Drevision=0.1.0" verify
```

The manual `Release` workflow also accepts a `revision` input. For tag-triggered
releases, the workflow derives the revision from the tag name by removing the
leading `v`.

The `Release` GitHub Actions workflow is manual/tag-triggered and does not run
for pull requests. It always imports the configured GPG key, signs release
artifacts, and uploads the generated SBOM bundle and detached signatures as a
workflow artifact. On tag pushes, or on manual runs with `publish=true`, it also
attaches the SBOMs and signatures to the matching GitHub Release. Manual publish
runs require the matching `vX.Y.Z` tag to already exist; the workflow will not
create release tags implicitly. Central publication is disabled unless the
manual `publish` input is set and repository credentials are configured.

Expected publishing configuration:

- secret `MAVEN_CENTRAL_TOKEN_USERNAME`
- secret `MAVEN_CENTRAL_TOKEN_PASSWORD`
- secret `MAVEN_GPG_PRIVATE_KEY`
- secret `MAVEN_GPG_PASSPHRASE`

Publish credentials must be Central Portal user-token credentials for the
`central` Maven server id. The matching GPG public key must be published before
the first live release.

The `Javadoc Pages` workflow runs on `vX.Y.Z` tag pushes. It builds aggregate
Javadoc with `./mvnw -B -ntp -DskipTests -Djacoco.skip=true javadoc:aggregate`,
publishes the API reference to the `gh-pages` branch under `/api/<version>/`,
and updates `/api/latest/` only when that tag is the highest published semantic
version.
