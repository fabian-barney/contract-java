# Java Contracts Specification

## 1. Introduction

This project provides declarative contracts for Java applications by expressing preconditions and postconditions with annotations. Contracts describe invariants that must hold if the code is correct. A contract violation is a programming error, not invalid user input.

Version 1 of this specification focuses on method and constructor contracts:

- Parameter contracts are enforced as preconditions.
- Method contracts are enforced as postconditions on non-void return values.
- Field annotations are supported for metadata reuse and Lombok propagation, but field invariants are not enforced directly in v1.

Spring Boot is an optional integration layer for convenient dependency management. The contract model itself is framework-agnostic and is intended to work in any Java project.

### Contracts vs. Validation

| Aspect | Jakarta Bean Validation | Contracts (this framework) |
|---|---|---|
| Purpose | Validate untrusted external input | Catch programming errors |
| Violation means | Bad data, expected and recoverable | Bug in calling or called code |
| Exception type | `ConstraintViolationException` | Standard JDK `RuntimeException` subtypes |
| Typical location | Controller or API boundary | Internal method and constructor boundaries |
| Analogy | Input sanitization | `Objects.requireNonNull()`, Guava `Preconditions`, assertions |

Jakarta Bean Validation answers "is this data acceptable?". This framework answers "is this code correct?".

### Design Lineage

This framework is the declarative counterpart to imperative contract utilities:

- `Objects.requireNonNull(obj)` - JDK, throws `NullPointerException`
- `Preconditions.checkNotNull(obj)` - Guava, throws `NullPointerException`
- `Preconditions.checkArgument(expr)` - Guava, throws `IllegalArgumentException`
- `Preconditions.checkState(expr)` - Guava, throws `IllegalStateException`
- `Assert.notNull(obj, msg)` - Spring, throws `IllegalArgumentException`

Instead of imperative checks in each method body, contracts are declared through annotations and enforced automatically.

---

## 2. Annotation API

### 2.1 The `Contract` Annotation

`Contract` is a Java annotation type (`@interface`) with two roles:

1. Namespace - all built-in contract annotations are nested inside `Contract`, such as `@Contract.NotNull` and `@Contract.Positive`.
2. Meta-annotation - `@Contract` may be placed on custom annotation types to mark them as contract annotations.

```java
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface Contract {

    // Built-in contract annotations are nested here.
}
```

A single import, `import <package>.Contract;`, gives access to the built-in contract annotations.

### 2.2 Supported Placements and v1 Semantics

| Placement | Semantics | v1 status |
|---|---|---|
| `PARAMETER` | Precondition on a method or constructor argument | Enforced |
| `METHOD` | Postcondition on a non-void return value | Enforced |
| `FIELD` | Contract metadata on fields, primarily for Lombok propagation and shared declarations | Supported but not directly enforced |
| `ANNOTATION_TYPE` | Enables custom composed contract annotations | Supported |

```java
@Contract.NotNull
public String findUser(
        @Contract.Positive Integer limit,
        @Contract.Nullable @Contract.Positive Integer softLimit) {
    // ...
}
```

In the example above:

- the method-level `@Contract.NotNull` is a postcondition,
- `limit` is non-null by default and must be positive,
- `softLimit` is nullable, and if it is non-null it must be positive.

### 2.3 Nullability Model

Nullability is defined independently from all other contract semantics.

- Parameters and non-void return values are non-null by default.
- `@Contract.NotNull` explicitly declares non-null behavior.
- `@Contract.Nullable` explicitly declares nullable behavior.
- Direct JSpecify `@org.jspecify.annotations.NonNull` and `@org.jspecify.annotations.Nullable` on parameter types and return types are treated as first-class nullness inputs with the same meaning as `@Contract.NotNull` and `@Contract.Nullable`.
- JSpecify `@NullMarked` and `@NullUnmarked` are not part of v1 contract semantics and do not affect generated checks.
- If nullability declarations conflict, compilation must fail.
- Only nullability declarations decide whether `null` is accepted or rejected.
- All non-nullability contracts are null-safe for object types: if the value is `null`, those contracts are skipped and the nullability rule decides the outcome.

Examples:

- `@Contract.Positive Integer amount` means `amount` must be non-null and positive because the default nullability is non-null.
- `@Contract.Nullable @Contract.Positive Integer amount` means `null` is allowed, and positive is checked only when `amount` is non-null.
- `public @org.jspecify.annotations.Nullable String findUser(...)` means the return value may be `null` without using `@Contract.Nullable`.

### 2.4 Built-in Contract Annotations

#### Nullability and Confidentiality

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.NotNull` | Any reference type | The value must not be `null` |
| `@Contract.Nullable` | Any reference type | The value may be `null` |
| `@Contract.Mask` | Any value that may appear in a generated violation message | The raw value must not appear in generated exception messages |

`@Contract.Mask` is not itself a validity check. It modifies exception-message rendering for the annotated value. It may be used directly or as a meta-annotation in a custom contract.

#### String and Content Checks

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.NotEmpty` | `String`, `CharSequence`, `Collection`, `Map`, arrays | If non-null, the value must not be empty |
| `@Contract.NotBlank` | `String`, `CharSequence` | If non-null, the value must contain at least one non-whitespace character |

`@Contract.NotEmpty` and `@Contract.NotBlank` do not define null behavior. If the value is `null`, the nullability rule applies first and the content check is skipped.

#### Numeric Checks

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.Positive` | Primitive numeric types, `BigDecimal`, `BigInteger`, and numeric wrapper types | If present, the value must be `> 0` |
| `@Contract.Negative` | Same | If present, the value must be `< 0` |
| `@Contract.NonNegative` | Same | If present, the value must be `>= 0` |
| `@Contract.NonPositive` | Same | If present, the value must be `<= 0` |
| `@Contract.InRange` | Same | If present, the value must be within the specified range |

`@Contract.InRange` attributes:

```java
@Contract.InRange(min = 0, max = 100)
@Contract.InRange(min = 0, max = 100, minInclusive = true, maxInclusive = false)
```

For wrapper types, `null` is handled only by the nullability rule. The numeric contract runs only when the wrapper value is non-null.

#### Size Checks

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.Size` | `String`, `CharSequence`, `Collection`, `Map`, arrays | If non-null, the length or size must be within `[min, max]` |

```java
@Contract.Size(min = 1, max = 255)
```

`@Contract.Size` does not imply non-null.

#### Pattern Checks

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.Pattern` | `String`, `CharSequence` | If non-null, the value must match the given regular expression |

```java
void setEmail(@Contract.Pattern(regexp = "^[^@]+@[^@]+$") String email)
```

`@Contract.Pattern` does not imply non-null.

### 2.5 Custom Contract Annotations

Users may define their own contract annotations by composing built-in annotations and marking them with `@Contract`.

```java
@Contract
@Contract.NotNull
@Contract.Positive
@Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface ValidId {
    String message() default "must be a valid ID";
}
```

Usage:

```java
public void deleteUser(@ValidId Long userId) {
    // ...
}
```

The annotation processor detects annotations meta-annotated with `@Contract` and enforces their composed contract semantics on supported targets.

### 2.6 Common Annotation Attributes

Every built-in contract annotation that can fail exposes:

```java
String message() default "";
```

When `message` is non-empty, that message replaces the generated contract description.

`@Contract.Mask` additionally exposes an optional renderer attribute for a custom mask renderer type. The framework-provided default renderer must be conservative and must not reveal the original value or any sensitive substring of it.

### 2.7 Annotation Definition Summary

All built-in contract annotations are documented and retained at runtime.

Built-in contract annotations target:

```java
@Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
```

`@Contract` itself targets only `ANNOTATION_TYPE`.

---

## 3. Exception Behavior

### 3.1 Exception Types

Contract violations throw standard JDK exceptions.

#### Precondition Violations

| Condition | Exception |
|---|---|
| Nullability rule failed for a parameter | `NullPointerException` |
| Any non-nullability precondition failed | `IllegalArgumentException` |

Nullability-rule failures include:

- the implicit non-null default,
- `@Contract.NotNull`,
- direct JSpecify `@NonNull`,
- null rejection that is implied when a parameter is non-null by default.

#### Postcondition Violations

| Condition | Exception |
|---|---|
| Any method postcondition failed, including a nullability rule | `IllegalStateException` |

### 3.2 Exception Messages

Generated exception messages include:

- the fully qualified method name,
- the parameter name for preconditions,
- the violated contract,
- the actual violating value when value rendering is allowed.

Unmasked examples:

```text
Parameter 'id' of method 'com.example.UserService.findUser' must not be null

Parameter 'limit' of method 'com.example.UserService.findUser' must be positive, but was: -3
```

Masked examples:

```text
Parameter 'password' of method 'com.example.AccountService.login' must match the required pattern, but was: [MASKED]

Postcondition of method 'com.example.TokenService.issue' violated: return value must not be blank, but was: [MASKED]
```

Masking rules:

- If `@Contract.Mask` applies to a parameter or return value, generated messages must not include the raw value.
- The default mask renderer must be intentionally non-informative.
- A custom mask renderer may provide an approved masked representation, but it must still avoid leaking confidential data.

When a contract annotation's `message` attribute is set, it replaces the generated contract description but does not disable location information or masking.

```java
void transfer(@Contract.Positive(message = "transfer amount must be positive") BigDecimal amount)
```

```text
Parameter 'amount' of method 'com.example.AccountService.transfer': transfer amount must be positive, but was: 0
```

### 3.3 Parameter Name Resolution

For meaningful error messages, parameter names are resolved in this order:

1. The `-parameters` compiler flag, which preserves parameter names in bytecode.
2. Fallback names such as `arg0`, `arg1`, and so on.

---

## 4. Enforcement Mechanism

### 4.1 Annotation Processor

Contracts are enforced by a compile-time annotation processor that generates checks directly into the relevant methods and constructors. This approach:

- works for plain Java applications and is not limited to Spring,
- has no runtime reflection or proxy requirement,
- works for constructors, private methods, and self-invocation,
- generates unconditional checks, similar in spirit to Lombok's generated null checks.

Version 1 enforcement is limited to supported parameter and method targets. Field annotations are part of the public annotation model but are not enforced directly as field invariants in v1.

### 4.2 Generated Code

The annotation processor generates code equivalent to manual precondition checks at the start of the method body and postcondition checks before every return path.

Source:

```java
@Contract.NotNull
public String findUser(
        @Contract.Positive Integer limit,
        @Contract.Nullable @Contract.Positive Integer softLimit) {
    return repository.find(limit, softLimit);
}
```

Equivalent after processing:

```java
public String findUser(Integer limit, Integer softLimit) {
    // Generated nullability check from the implicit non-null default
    if (limit == null) {
        throw new NullPointerException(
            "Parameter 'limit' of method 'com.example.UserService.findUser' must not be null");
    }

    // Generated semantic contract checks
    if (limit <= 0) {
        throw new IllegalArgumentException(
            "Parameter 'limit' of method 'com.example.UserService.findUser' must be positive, but was: " + limit);
    }
    if (softLimit != null && softLimit <= 0) {
        throw new IllegalArgumentException(
            "Parameter 'softLimit' of method 'com.example.UserService.findUser' must be positive, but was: " + softLimit);
    }

    String $result = repository.find(limit, softLimit);

    if ($result == null) {
        throw new IllegalStateException(
            "Postcondition of method 'com.example.UserService.findUser' violated: return value must not be null, but was: null");
    }
    return $result;
}
```

If `contracts.defaultNotNull=false`, the generated null check for `limit` in the example above would be omitted unless null rejection were declared explicitly through `@Contract.NotNull` or direct JSpecify `@NonNull`.

### 4.3 Annotation Processor Options

The annotation processor supports compiler options for behavior control.

| Option | Default | Description |
|---|---|---|
| `contracts.enabled` | `true` | Master switch to enable or disable code generation |
| `contracts.defaultNotNull` | `true` | Makes parameters and non-void return values non-null by default |

Options are passed through the build tool.

Maven:

```xml
<compilerArgs>
    <arg>-Acontracts.enabled=true</arg>
    <arg>-Acontracts.defaultNotNull=true</arg>
</compilerArgs>
```

Gradle (Kotlin DSL):

```kotlin
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Acontracts.enabled=true")
    options.compilerArgs.add("-Acontracts.defaultNotNull=true")
}
```

### 4.4 Compile-Time Validation

The annotation processor must reject invalid annotation usage with compiler errors, including:

- `@Contract.NotNull` or `@Contract.Nullable` on a primitive parameter or primitive return type,
- `@Contract.Positive` on a `String`,
- `@Contract.NotBlank` on a `Collection`,
- any method contract on a `void` method,
- conflicting nullability declarations on the same parameter or return value, such as `@Contract.Nullable` together with `@Contract.NotNull`,
- conflicting framework and direct JSpecify nullability declarations on the same parameter or return value.

### 4.5 Null-Safe Evaluation and Wrapper Types

For object types, non-nullability contracts never decide null acceptance. Their evaluation rules are:

- If the value is non-null, the contract is evaluated normally.
- If the value is `null`, the contract is skipped.
- The nullability rule then determines whether `null` is allowed.

For numeric wrapper types such as `Integer`, `Long`, `Double`, and `BigDecimal`:

- `@Contract.Positive`, `@Contract.InRange`, and other numeric contracts may be applied even when the value is nullable.
- `null` passes the numeric contract when the parameter or return value is nullable.
- `null` fails only when the parameter or return value is non-null by default or declared non-null explicitly.

---

## 5. Compatibility

### 5.1 JSpecify Compatibility

JSpecify is a first-class nullability input for this framework, but only at direct parameter and return declarations in v1.

- Direct `@org.jspecify.annotations.Nullable` is treated the same as `@Contract.Nullable`.
- Direct `@org.jspecify.annotations.NonNull` is treated the same as `@Contract.NotNull`.
- `@NullMarked` and `@NullUnmarked` are outside the v1 contract model and do not drive code generation.
- JSpecify remains optional. Projects that do not depend on JSpecify can use the framework-native nullability annotations only.

This allows downstream projects to use JSpecify annotations for null handling while continuing to use `@Contract.*` for semantic contracts such as positivity, size, and pattern matching.

### 5.2 Lombok Compatibility

This framework must support Lombok workflows that rely on `lombok.copyableAnnotations`.

Normative requirements:

- Built-in contract annotations support `FIELD` so they can be placed on fields and copied by Lombok.
- Contract annotations copied by Lombok onto generated constructor parameters, setters, getters, builder methods, or other generated members are treated exactly as if the user had written them directly on those supported targets.
- The specification does not require direct enforcement of field declarations in v1.

Recommended downstream usage:

- Declare contracts on fields when that is the best source of truth for Lombok-generated APIs.
- Add the relevant contract annotations to `lombok.copyableAnnotations` so Lombok propagates them to generated code.

---

## 6. Deliverables and Spring Boot Integration

### 6.1 Public Deliverables

The public packaging model consists of two user-facing deliverables:

| Deliverable | Purpose |
|---|---|
| `contract-core` | Framework-agnostic contract annotations and enforcement support for any Java project |
| `contract-spring-boot-starter` | Spring Boot wrapper that brings in the contract framework with Spring-friendly dependency management |

The internal implementation may still use more than two modules, but the public specification is defined in terms of these two deliverables.

### 6.2 Coordinates

The documented Maven `groupId` is:

```text
media.barney
```

Example coordinates:

```xml
<dependency>
    <groupId>media.barney</groupId>
    <artifactId>contract-core</artifactId>
    <version>TBD</version>
</dependency>
```

```xml
<dependency>
    <groupId>media.barney</groupId>
    <artifactId>contract-spring-boot-starter</artifactId>
    <version>TBD</version>
</dependency>
```

### 6.3 Spring Boot Support Policy

The Spring Boot starter supports only Spring Boot release lines that are still within active OSS support according to Spring's published support policy.

As of April 18, 2026, that means Spring Boot `4.0.x` and `3.5.x`. This dated note is descriptive context, not a permanent minimum-version rule. The normative rule is to follow the current OSS support window.

During implementation, the concrete supported Spring Boot lines should be captured in an ADR so that the codebase and release process can be updated when Spring changes its support window.

The contract model itself does not depend on Spring and remains usable through `contract-core` in non-Spring projects.

---

## 7. Requirements

- Java 17+
- Maven 3.9+ or Gradle 8+
- Spring Boot starter support is limited to Spring Boot release lines with current OSS support

