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

## Releases

Do not include version bumps in ordinary feature, fix, or documentation PRs.
Version changes belong in release or snapshot-bump PRs so the changelog,
artifact metadata, tag, and published artifacts stay auditable as one release
unit.

The reactor version is controlled by the root `pom.xml` `revision` property.
Change it by manually editing that single property; do not use
`versions-maven-plugin set-property` unless a future issue explicitly adds that
workflow. Child modules should keep `${revision}` in their parent declarations.
For a release PR, set `revision` to the release version such as `1.2.3`. After
the release is published, open a dedicated snapshot-bump PR that sets
`revision` to the next patch snapshot such as `1.2.4-SNAPSHOT`.

Every release PR must update `CHANGELOG.md`: move completed `Unreleased` notes
into the target version section, add the release date, and leave a fresh
`Unreleased` section for the next development cycle. Review the changelog before
tagging or publishing a release. The release workflow rejects target version
sections that still use the `TBD` date marker.

The `release` Maven profile attaches source and Javadoc JARs, generates
CycloneDX XML and JSON SBOMs, and flattens published POMs. Use unsigned local
verification; publication CI normalizes, signs, and retains the exact payload:

```sh
./mvnw -B -ntp -Prelease -Drevision=1.2.3 -Dgpg.skip=true verify
```

In PowerShell, quote dotted `-D` properties:

```powershell
.\mvnw.cmd -B -ntp -Prelease "-Drevision=1.2.3" "-Dgpg.skip=true" verify
```

The reviewed stable-version PR merge starts `Release` from protected `main`.
It waits for `verify / required` on that exact SHA, validates coordinates and
release notes, creates a signed annotated tag, and continues within the same
workflow. Unchanged revisions and snapshot bumps publish nothing.

Workflow dispatch runs a nonpublishing preflight. It compares two clean builds
and exercises the same release controls and packaging, without publishing
credentials, tags, or releases.

The publication job uploads the retained Central ZIP through the Portal API,
records its deployment ID, and verifies downloadable bytes, signatures,
checksums, and GitHub provenance/SBOM attestations. It explicitly calls the
reusable `Javadoc Pages` workflow, which deploys the retained Javadoc archives
with Actions Pages, preserving `/api/<version>/` and selecting `/api/latest/`
by semantic version. GitHub finalization follows complete verification.

See [RELEASING.md](RELEASING.md) for repository configuration, the pinned signing
fingerprint, fourteen-asset layout, consumer verification, and recovery rules.
