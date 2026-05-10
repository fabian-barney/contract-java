# ADR-0002: Spring Boot starter support window

## Status

Accepted.

## Context

`contract-spring-boot-starter` is a dependency-management convenience wrapper
around `contract-core`. The contract model is framework-agnostic, but the
starter should only document and smoke-test Spring Boot release lines that are
inside Spring's current OSS support window.

Spring's published support policy maps project support dates to Spring Boot and
states that Spring Boot minor releases receive OSS support for at least 13
months. The current Spring Boot system requirements documentation shows Spring
Boot 4.0.6 as the latest stable track and Spring Boot 3.5.14 as the latest 3.5
track. Both require Java 17 or later.

Sources checked on 2026-05-10:

- https://spring.io/support-policy/
- https://docs.spring.io/spring-boot/system-requirements.html
- https://docs.spring.io/spring-boot/3.5/system-requirements.html

## Decision

As of 2026-05-10, `contract-spring-boot-starter` supports Spring Boot `4.0.x`
and `3.5.x` as its documented OSS-supported Spring Boot lines.

The concrete smoke-test versions are:

- Spring Boot `4.0.6`
- Spring Boot `3.5.14`

The support rule is not permanently tied to those versions. The project should
refresh the ADR and smoke-test versions when Spring changes its OSS support
window or publishes a new supported line.

## Consequences

The starter remains a thin wrapper that depends on `contract-core` and does not
add Spring runtime behavior. CI validates dependency resolution against the
supported Spring Boot lines using the Spring Boot dependency BOMs.

Older Spring Boot lines may still work because `contract-core` has no Spring
runtime dependency, but they are outside the documented support target for the
starter.
