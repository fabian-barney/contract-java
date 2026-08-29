# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## 0.1.9 - 2026-08-29

### Changed

- Updated the GitHub Actions checkout and Java setup actions to 7.0.1 and
  5.7.0.
- Updated the Spring Boot 4.1 dependency BOM from 4.1.0 to 4.1.1 and the
  JUnit BOM from 6.1.2 to 6.1.3.
- Updated the cognitive-java Maven plugin from 0.6.0 to 0.7.0.
- Restored the CRAP quality gate to the default threshold supplied by
  `crap-java` 0.6.3 instead of maintaining a project override.
- Kept release and compatibility verification aligned with the updated
  dependency and workflow tooling.

## 0.1.8 - 2026-08-29

### Changed

- Updated `com.diffplug.spotless:spotless-maven-plugin` from 3.9.0 to 3.10.0.

## 0.1.7 - 2026-08-09

### Changed

- Updated the Spring Boot starter support matrix to Spring Boot `4.0.x` and
  `4.1.x`, and dropped the near-EOL `3.5.x` smoke-test line.

### Fixed

- Ensured constructor preconditions follow explicit constructor invocations and
  custom mask renderer `Error` subtypes propagate instead of being swallowed.

## 0.1.6 - 2026-08-03

### Changed

- Updated `org.jspecify:jspecify` from 1.0.0 to 1.0.1.

## 0.1.5 - 2026-07-19

### Changed

- Updated `org.junit:junit-bom` from 6.1.0 to 6.1.1.

## 0.1.4 - 2026-06-28

### Changed

- Updated `com.uber.nullaway:nullaway` from 0.13.6 to 0.13.7.

## 0.1.3 - 2026-06-28

### Changed

- Updated `org.cyclonedx:cyclonedx-maven-plugin` from 2.9.1 to 2.9.2.

## 0.1.2 - 2026-06-28

### Changed

- Updated `com.diffplug.spotless:spotless-maven-plugin` from 3.6.0 to 3.7.0.

## 0.1.1 - 2026-06-19

### Changed

- Updated org.jacoco:jacoco-maven-plugin from 0.8.14 to 0.8.15.
- Updated com.uber.nullaway:nullaway from 0.13.4 to 0.13.6.
- Updated com.google.errorprone:error_prone_core from 2.49.0 to 2.50.0.

## 0.1.0 - 2026-06-01

### Added

- Initial `contract-core` annotation API for declarative Java preconditions,
  postconditions, masking, range, size, pattern, and sign contracts.
- Annotation-processor-generated runtime checks with generated-code bridge
  support in `media.barney.contract.runtime`.
- Spring Boot starter with auto-configuration, actuator info contribution, and
  opt-in HTTP 500 handling for detected generated contract violations.
- Maven, Gradle, and Spring Boot starter installation documentation.
- Example module covering end-user contract usage in CI without publishing
  examples as Central artifacts.
- CI coverage for supported JDK and Spring Boot lines, static analysis,
  formatting, CRAP score, cognitive complexity, and Codecov upload.
- Contributor, security, issue, pull-request, and repository workflow
  documentation for the first public release line.

### Changed

- Established the public API stability policy for maintained packages,
  generated-code bridge behavior, internal packages, and pre-`1.0.0`
  compatibility expectations.

[0.1.9]: https://github.com/fabian-barney/contract-java/compare/v0.1.8...v0.1.9
[0.1.8]: https://github.com/fabian-barney/contract-java/compare/v0.1.7...v0.1.8
[0.1.7]: https://github.com/fabian-barney/contract-java/compare/v0.1.6...v0.1.7
[0.1.6]: https://github.com/fabian-barney/contract-java/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/fabian-barney/contract-java/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/fabian-barney/contract-java/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/fabian-barney/contract-java/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/fabian-barney/contract-java/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/fabian-barney/contract-java/compare/v0.1.0...v0.1.1
[Unreleased]: https://github.com/fabian-barney/contract-java/compare/v0.1.9...HEAD
