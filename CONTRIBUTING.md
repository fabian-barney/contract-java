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
against the supported Spring Boot `3.5.x` and `4.0.x` BOMs.

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

The `release` Maven profile attaches source and Javadoc jars and includes a
GPG signing hook that is skipped by default:

```sh
./mvnw -B -ntp -Prelease -DskipTests -Dgpg.skip=true package
```

In PowerShell, quote dotted `-D` properties:

```powershell
.\mvnw.cmd -B -ntp -Prelease -DskipTests "-Dgpg.skip=true" package
```

The `Release` GitHub Actions workflow is manual/tag-triggered and does not run
for pull requests. Publishing is disabled unless the manual `publish` input is
set and repository credentials are configured.

Expected publishing configuration:

- repository variable `MAVEN_RELEASE_REPOSITORY_URL`
- secret `MAVEN_USERNAME`
- secret `MAVEN_PASSWORD`

Signing can be enabled later by setting `gpg.skip=false` and providing the
required GPG key handling in the release workflow.
