# ADR-0002: Spring Boot starter support window

## Status

Accepted. Starter-purpose statements are superseded by
[ADR-0003](ADR-0003-SPRING-BOOT-STARTER-PURPOSE.md); the support-window decision remains.

## Context

`contract-spring-boot-starter` integrates `contract-core` with Spring Boot through
auto-configuration and optional web/actuator behavior defined in ADR-0003.
The contract model is framework-agnostic, but the
starter should only document and smoke-test Spring Boot release lines that are
inside Spring's current OSS support window.

Spring's published support policy maps project support dates to Spring Boot and
states that Spring Boot minor releases receive OSS support for at least 13
months. The current Spring Boot system requirements documentation shows Spring
Boot 4.1.0 as the latest stable track and Spring Boot 4.0.7 as the latest 4.0
track. Both require Java 17 or later.

Sources checked on 2026-06-21:

- https://spring.io/support-policy/
- https://docs.spring.io/spring-boot/system-requirements.html
- https://endoflife.date/spring-boot

## Decision

As of 2026-06-21, `contract-spring-boot-starter` supports Spring Boot `4.0.x`
and `4.1.x` as its documented OSS-supported Spring Boot lines.

Spring Boot `3.5.x` reaches OSS end of life on 2026-06-30. Because the project
is still in the `0.x` development line, the starter drops documented `3.5.x`
support as part of this support-window refresh.

The concrete smoke-test versions when this decision was accepted were:

- Spring Boot `4.0.7`
- Spring Boot `4.1.0`

Current patch versions are pinned in the root POM and exercised by CI. The
`1.0.0` release preserves the supported `4.0.x` and `4.1.x` lines.

The support rule is not permanently tied to those versions. The project should
refresh the ADR and smoke-test versions when Spring changes its OSS support
window or publishes a new supported line.

## Consequences

The starter depends on `contract-core` and contributes conditional
auto-configuration, an opt-in servlet exception resolver, and actuator
information. Enforcement remains in the core processor/runtime bridge.
CI validates behavior and dependency resolution against the supported Spring
Boot lines using their dependency BOMs.

Older Spring Boot lines may still work because `contract-core` has no Spring
runtime dependency, but they are outside the documented support target for the
starter.
