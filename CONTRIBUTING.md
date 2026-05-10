# Contributing

## Local Checks

Run the same verification as the default CI gate:

```sh
./mvnw -B -ntp verify
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
