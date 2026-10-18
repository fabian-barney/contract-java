# Migrating to 2.0.0

Version 2.0.0 raises the minimum Java version to 21. Official support
covers JDK 21 and 25 with current security patches; other Java releases are
unverified. This is a deliberate support policy change, not a claim that every
vendor's Java 17 distribution has reached end of life.

## Application changes

- Move builds and annotation processing to a full JDK 21 or 25, and deploy on
  Java 21 or 25. The library JARs use Java 21 bytecode (class-file major 65).
- Set Maven compiler `release` or Gradle toolchain/target compatibility to 21
  or the supported JDK line used by the application.
- Keep explicit annotation-processor configuration and the documented javac
  exports. Access to javac internals is still required on supported JDKs.
- Keep the existing annotation APIs, generated runtime bridge calls, exception
  handling, masking, and Spring starter property names/defaults. These contracts
  are unchanged by the baseline migration. Existing generated code is checked
  against the new runtime on supported JDKs.

Spring Boot 4.0.x and 4.1.x remain supported. This migration does not require
switching between those lines or overriding their managed production dependencies.

## Repository builds

Use Maven 3.9.16 or newer; the wrapper remains pinned to Maven 3.10.0. Both quality
plugins now use 2.0.0 and require Java 21 and Maven 3.9.16. Their default quality
thresholds remain unchanged. If external tooling consumes their TOON reports,
review the upstream 2.0 migration notes for the adopted JToon 3 semantics.

JUnit API, engine, and platform resolve consistently to 6.1.3 in every module.
The existing `spring.boot.smoke.line=4.0` selector now controls the starter's
compile/test BOM as well as the isolated resolution check; the default uses 4.1.

Upgrade both `contract-core` and `contract-spring-boot-starter`, when used, to
`2.0.0`. Published 1.x artifacts and their historical compatibility remain
unchanged. See [CHANGELOG.md](CHANGELOG.md) for the complete 2.0.0 release notes.
