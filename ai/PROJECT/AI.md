# contract-java AI Guidance

This project uses the vendored ai-rules baseline at `../AI-RULES/AI.md`.
Project-specific guidance extends that baseline from this directory.

## Project Context

`contract-java` provides declarative Java contracts through annotations,
compile-time enforcement, and an optional Spring Boot starter. The specification
source of truth is `spec.md` until implementation documentation supersedes it.

## Documentation Rules

- Rule and policy docs state current rules only: no "as of" notes and no
  records of past decisions. History belongs in the CHANGELOG, git, and issues.
- Docs name supported lines, such as Spring Boot `4.1.x` or JDK 25, never pinned
  patch or tool versions. Pins live only in build files. Supported minimums,
  such as the minimum Maven version, and the release version in installation
  examples are not pins.

## Project-Owned Knowledge

- [Lessons Learned](LESSONS_LEARNED/LESSONS_LEARNED.md)
- [Architecture Decisions](DECISIONS/DECISIONS.md)
- [Release, support, and versioning policy](../../RELEASING.md)
