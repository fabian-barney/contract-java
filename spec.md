# Spring Boot Method Contracts — Specification

## 1. Introduction

This project provides **declarative method contracts** for Spring Boot applications using annotations. Contracts express invariants that must hold if the code is correct. A contract violation is a **programming error** (bug), not invalid user input.

### Contracts vs. Validation

| Aspect | Jakarta Bean Validation | Method Contracts (this framework) |
|---|---|---|
| **Purpose** | Validate untrusted external input | Catch programming errors (bugs) |
| **Violation means** | Bad data — expected, recoverable | Bug in calling or called code — unexpected, non-recoverable |
| **Exception type** | `ConstraintViolationException` | Standard JDK `RuntimeException` subtypes |
| **Typical location** | Controller / API boundary | Internal service-to-service calls |
| **Analogy** | Input sanitization | `assert`, `Objects.requireNonNull()`, Guava `Preconditions` |

Jakarta Bean Validation answers *"is this data acceptable?"*. This framework answers *"is this code correct?"*.

### Design Lineage

This framework is the **declarative** counterpart to imperative contract utilities:

- `Objects.requireNonNull(obj)` — JDK, throws `NullPointerException`
- `Preconditions.checkNotNull(obj)` — Guava, throws `NullPointerException`
- `Preconditions.checkArgument(expr)` — Guava, throws `IllegalArgumentException`
- `Preconditions.checkState(expr)` — Guava, throws `IllegalStateException`
- `Assert.notNull(obj, msg)` — Spring, throws `IllegalArgumentException`

Instead of imperative calls at the top of every method, contracts are declared via annotations and enforced automatically at compile time.

---

## 2. Annotation API

### 2.1 The `Contract` Annotation

`Contract` is a Java annotation type (`@interface`) that serves two purposes:

1. **Namespace** — All built-in contract annotations are nested inside `Contract` (e.g., `@Contract.NotNull`, `@Contract.Positive`).
2. **Meta-annotation** — `@Contract` itself can be placed on custom annotation types to mark them as contract annotations (see [Section 2.4](#24-custom-contract-annotations)).

```java
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface Contract {

    // Built-in contract annotations are nested here (see below).
}
```

A single import — `import <package>.Contract;` — gives access to all contract annotations.

### 2.2 Precondition and Postcondition Semantics

Every contract annotation supports **two placements** that determine its semantics:

| Placement | Semantics | Who is at fault on violation |
|---|---|---|
| On a **parameter** | **Precondition** — the caller must satisfy this | The caller (bug in calling code) |
| On a **method** | **Postcondition** — the method guarantees this about its return value | The callee (bug in method implementation) |

```java
@Contract.NotNull                                    // Postcondition: never returns null
public String findUser(@Contract.NotNull String id,  // Precondition: id must not be null
                       @Contract.Positive int limit) // Precondition: limit must be > 0
{
    // ...
}
```

All contract annotations therefore target both `ElementType.PARAMETER` and `ElementType.METHOD` (plus `ElementType.ANNOTATION_TYPE` for composition).

### 2.3 Built-in Contract Annotations

#### Null Checks

| Annotation | Applies to | Precondition semantics | Postcondition semantics |
|---|---|---|---|
| `@Contract.NotNull` | Any reference type | Parameter must not be `null` | Return value must not be `null` |

#### String / CharSequence Checks

| Annotation | Applies to | Precondition semantics | Postcondition semantics |
|---|---|---|---|
| `@Contract.NotEmpty` | `String`, `CharSequence`, `Collection`, `Map`, arrays | Must not be `null` and must not be empty | Return value must not be `null` or empty |
| `@Contract.NotBlank` | `String`, `CharSequence` | Must not be `null` and must contain at least one non-whitespace character | Return value must not be `null` or blank |

`@Contract.NotEmpty` and `@Contract.NotBlank` imply a null check. If the value is `null`, the null check fails first (see [Section 3](#3-exception-behavior) for exception types).

#### Numeric Checks

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.Positive` | `int`, `long`, `double`, `float`, `short`, `byte`, `BigDecimal`, `BigInteger`, and their wrappers | Value must be `> 0` |
| `@Contract.Negative` | (same) | Value must be `< 0` |
| `@Contract.NonNegative` | (same) | Value must be `>= 0` |
| `@Contract.NonPositive` | (same) | Value must be `<= 0` |
| `@Contract.InRange` | (same) | Value must be within the specified range |

`@Contract.InRange` attributes:

```java
@Contract.InRange(min = 0, max = 100)    // inclusive by default
@Contract.InRange(min = 0, max = 100,
    minInclusive = true,                  // default: true
    maxInclusive = false)                 // half-open range: [0, 100)
```

#### Size Checks

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.Size` | `String`, `CharSequence`, `Collection`, `Map`, arrays | Length or size must be within `[min, max]` |

```java
@Contract.Size(min = 1, max = 255)    // min defaults to 0, max defaults to Integer.MAX_VALUE
```

`@Contract.Size` does **not** imply a null check. If the value may be `null`, combine with `@Contract.NotNull`.

#### Pattern Checks

| Annotation | Applies to | Semantics |
|---|---|---|
| `@Contract.Pattern` | `String`, `CharSequence` | Must match the given regular expression |

```java
void setEmail(@Contract.Pattern(regexp = "^[^@]+@[^@]+$") String email)
```

`@Contract.Pattern` does **not** imply a null check.

### 2.4 Custom Contract Annotations

Users can define their own contract annotations by composing built-in annotations and marking them with `@Contract`:

```java
@Contract                    // Marks this as a contract annotation
@Contract.NotNull            // Composed: must not be null
@Contract.Positive           // Composed: must be > 0
@Target({PARAMETER, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface ValidId {
    String message() default "must be a valid ID (non-null and positive)";
}
```

Usage:

```java
public void deleteUser(@ValidId Long userId) {
    // ...
}
```

The annotation processor detects annotations meta-annotated with `@Contract` and enforces all composed contract annotations found on them.

### 2.5 Common Annotation Attributes

Every built-in contract annotation has a `message` attribute:

```java
String message() default "";
```

When non-empty, the provided message is used in the exception. When empty (default), a descriptive message is generated automatically (see [Section 3.2](#32-exception-messages)).

### 2.6 Annotation Definition Summary

All built-in annotations share this structure:

```java
@Target({PARAMETER, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
```

- `PARAMETER` — precondition usage
- `METHOD` — postcondition usage
- `ANNOTATION_TYPE` — enables composition in custom contract annotations

---

## 3. Exception Behavior

### 3.1 Exception Types

Contract violations throw **standard JDK exceptions**, aligned with `Objects.requireNonNull()`, Guava `Preconditions`, and Effective Java conventions:

#### Precondition Violations (annotations on parameters)

| Condition | Exception |
|---|---|
| Null check failed (`@Contract.NotNull`, or the null-check part of `@Contract.NotEmpty` / `@Contract.NotBlank`) | `NullPointerException` |
| Any other precondition failed (`@Contract.Positive`, `@Contract.InRange`, `@Contract.Size`, `@Contract.Pattern`, or the emptiness/blankness part of `@Contract.NotEmpty` / `@Contract.NotBlank`) | `IllegalArgumentException` |

**Rationale:** `NullPointerException` for null arguments aligns with `Objects.requireNonNull()` (JDK), `Preconditions.checkNotNull()` (Guava), and Effective Java Item 72. `IllegalArgumentException` for other invalid arguments aligns with `Preconditions.checkArgument()` (Guava) and `Assert.isTrue()` (Spring).

#### Postcondition Violations (annotations on methods)

| Condition | Exception |
|---|---|
| Any postcondition failed (including null return) | `IllegalStateException` |

**Rationale:** A postcondition violation means the method implementation is broken — the method is in an illegal state. This aligns with `Preconditions.checkState()` (Guava) and `Assert.state()` (Spring).

### 3.2 Exception Messages

Exception messages are detailed and include:

- The fully qualified method name
- The parameter name (for preconditions)
- The violated contract
- The actual violating value (safely formatted)

**Precondition examples:**

```
Parameter 'id' of method 'com.example.UserService.findUser' must not be null

Parameter 'limit' of method 'com.example.UserService.findUser' must be positive, but was: -3

Parameter 'email' of method 'com.example.UserService.createUser' must not be blank, but was: "  "
```

**Postcondition examples:**

```
Postcondition of method 'com.example.UserService.findUser' violated: return value must not be null, but was: null

Postcondition of method 'com.example.UserService.getBalance' violated: return value must be non-negative, but was: -42
```

When the annotation's `message` attribute is set, it replaces the generated description:

```java
void transfer(@Contract.Positive(message = "transfer amount must be positive") BigDecimal amount)
```

```
Parameter 'amount' of method 'com.example.AccountService.transfer': transfer amount must be positive, but was: 0
```

### 3.3 Parameter Name Resolution

For meaningful error messages, parameter names are resolved in this order:

1. The `-parameters` compiler flag (recommended) — Java retains parameter names in bytecode.
2. Fallback to `arg0`, `arg1`, etc., if parameter names are not available.

---

## 4. Enforcement Mechanism

### 4.1 Annotation Processor

Contracts are enforced via a **compile-time annotation processor** that generates check code directly into annotated methods. This approach:

- Works for all classes, not just Spring beans
- Has zero runtime overhead beyond the checks themselves (no AOP proxies, no reflection)
- Works for constructors, private methods, and self-invocations
- Generates checks unconditionally — they always execute (like Lombok's `@NonNull`)

### 4.2 Generated Code

The annotation processor generates code equivalent to manual precondition checks at the start of the method body, and postcondition checks before every return point.

**Source:**

```java
@Contract.NotNull
public String findUser(@Contract.NotNull String id, @Contract.Positive int limit) {
    return repository.find(id, limit);
}
```

**Equivalent after processing:**

```java
public String findUser(String id, int limit) {
    // Generated precondition checks
    if (id == null) {
        throw new NullPointerException(
            "Parameter 'id' of method 'com.example.UserService.findUser' must not be null");
    }
    if (limit <= 0) {
        throw new IllegalArgumentException(
            "Parameter 'limit' of method 'com.example.UserService.findUser' must be positive, but was: " + limit);
    }

    // Original method body
    String $result = repository.find(id, limit);

    // Generated postcondition check
    if ($result == null) {
        throw new IllegalStateException(
            "Postcondition of method 'com.example.UserService.findUser' violated: "
            + "return value must not be null, but was: null");
    }
    return $result;
}
```

### 4.3 Annotation Processor Options

The annotation processor supports compiler options for controlling its behavior:

| Option | Default | Description |
|---|---|---|
| `contracts.enabled` | `true` | Master switch to enable/disable code generation |

Options are passed via the build tool:

**Maven:**

```xml
<compilerArgs>
    <arg>-Acontracts.enabled=true</arg>
</compilerArgs>
```

**Gradle (Kotlin DSL):**

```kotlin
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Acontracts.enabled=true")
}
```

### 4.4 Compile-Time Validation

The annotation processor validates annotation usage at compile time and produces **compiler errors** for misuse:

- `@Contract.NotNull` on a primitive parameter → error (primitives cannot be `null`)
- `@Contract.Positive` on a `String` parameter → error (not a numeric type)
- `@Contract.NotBlank` on a `Collection` parameter → error (not a `CharSequence`)
- `@Contract.Positive` on a method returning `void` → error (no return value to check)
- `@Contract.NotNull` on a method returning a primitive → error (primitives cannot be `null`)

### 4.5 Handling Wrapper Types

For numeric annotations (`@Contract.Positive`, `@Contract.NonNegative`, etc.) on wrapper types (`Integer`, `Long`, etc.):

- A `null` wrapper value causes a `NullPointerException` (precondition) or `IllegalStateException` (postcondition), since the numeric check cannot proceed on `null`.
- If null should be allowed, do not use numeric contract annotations on nullable parameters. Numeric contract annotations imply non-null.

---

## 5. Compatibility

### 5.1 JSpecify Compatibility

This framework **coexists peacefully** with JSpecify. There is no dependency on JSpecify and no runtime integration.

**Why they don't conflict:**

| Aspect | JSpecify | This framework |
|---|---|---|
| **Annotation names** | `@NonNull`, `@Nullable` | `@Contract.NotNull` |
| **Target** | `TYPE_USE` | `PARAMETER`, `METHOD` |
| **Purpose** | Static analysis tooling | Runtime enforcement via compile-time code generation |
| **Retention** | `RUNTIME` | `RUNTIME` |

Both can be used on the same parameter without conflict:

```java
import org.jspecify.annotations.NonNull;
import com.example.Contract;

public String findUser(@NonNull @Contract.NotNull String id) {
    // @NonNull    → static analysis (IDE warnings, NullAway, Error Prone)
    // @Contract.NotNull → runtime null check (generated by annotation processor)
}
```

Using both provides defense in depth: static analysis catches potential null issues at compile time, while generated contract checks catch actual null values at runtime.

### 5.2 Lombok Compatibility

This framework **coexists with Lombok** without interference. There is no dependency on Lombok.

**Lombok's `@NonNull`** generates an imperative null check (`if (param == null) throw new NullPointerException(...)`) via its own annotation processor. This framework's `@Contract.NotNull` generates a similar check via its annotation processor.

**If both are used on the same parameter**, two null checks are generated. The first one that executes will throw. This is harmless (redundant but not conflicting).

**Guidance for users of both:**

- Use **Lombok's `@NonNull`** for simple null checks where you already use Lombok.
- Use **`@Contract.NotNull`** (and other `@Contract.*` annotations) when you want the full contract annotation set, descriptive error messages, or postcondition support.
- Both can coexist on the same parameter if desired.

**Lombok-generated constructors** (`@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`): Lombok generates constructor source code before other annotation processors run, so `@Contract.*` annotations on constructor parameters of Lombok-generated constructors depend on the annotation processor ordering. Lombok's own `@NonNull` is the simpler choice for constructor null checks.

---

## 6. Spring Boot Integration

### 6.1 Starter Dependency

The project is packaged as a Spring Boot starter for convenient dependency management. Adding the starter brings in:

- The `Contract` annotation types
- The annotation processor (auto-registered via `META-INF/services/javax.annotation.processing.Processor`)

```xml
<!-- Maven -->
<dependency>
    <groupId>TBD</groupId>
    <artifactId>contract-spring-boot-starter</artifactId>
    <version>TBD</version>
</dependency>
```

```kotlin
// Gradle (Kotlin DSL)
implementation("TBD:contract-spring-boot-starter:TBD")
annotationProcessor("TBD:contract-spring-boot-starter:TBD")
```

### 6.2 Why a Spring Boot Starter?

Although the core mechanism is a compile-time annotation processor (no Spring dependency required), the project is packaged as a Spring Boot starter because:

- It follows the familiar Spring Boot dependency management convention.
- Spring Boot projects are the primary target audience.
- Future versions may add Spring-specific features (e.g., Actuator metrics for contract violations, integration with Spring's `ParameterNameDiscoverer`).

The annotation processor and annotations themselves have **no dependency on Spring**. They work in any Java project.

---

## 7. Module Structure

| Module | Contents | Dependencies |
|---|---|---|
| `contract-annotations` | `Contract` annotation type and all nested annotations | None (zero dependencies) |
| `contract-processor` | Annotation processor implementation | `contract-annotations`, Java compiler API |
| `contract-spring-boot-starter` | Spring Boot starter POM — brings in both modules | `contract-annotations`, `contract-processor` |

The annotations module is kept separate so that libraries can depend on the annotations alone (for API contracts) without pulling in the processor.

---

## 8. Requirements

- **Java**: 17+ (minimum for Spring Boot 3.x)
- **Spring Boot**: 3.2+ (for starter packaging; annotations and processor work without Spring)
- **Build tools**: Maven 3.9+ or Gradle 8+
