package media.barney.contract;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.apiguardian.api.API;

/**
 * Namespace and meta-annotation for declarative Java contracts.
 *
 * <p>Built-in contract annotations are nested under this type so a single
 * import gives callers access to declarations such as {@code @Contract.Positive}
 * and {@code @Contract.Pattern}. Custom composed annotations can be marked with
 * {@code @Contract} and may combine built-in annotations.
 *
 * <p>Built-in semantic contracts are supported on parameters, methods, fields,
 * and annotation types. Parameter annotations are enforced as preconditions,
 * method annotations are enforced as postconditions on non-void return values,
 * field annotations are metadata for reuse and tools such as Lombok, and
 * annotation-type annotations allow custom composed contracts.
 *
 * <p>Semantic contracts are null-safe for object values: when a supported value
 * is {@code null}, the contract is skipped and therefore passes. Nullness is
 * intentionally outside this framework; use dedicated nullness tooling for
 * required/non-null contracts.
 */
@API(status = API.Status.MAINTAINED)
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface Contract {

    /**
     * Marks a value as confidential for generated violation messages.
     *
     * <p>{@code Mask} is not a validity check. It changes only how generated
     * violation messages render the annotated parameter or return value. The raw
     * value must not appear in generated messages when this annotation applies.
     *
     * <p>This annotation can be used directly on supported program elements or
     * as part of a custom composed contract annotation. If the value is
     * {@code null}, masking still affects only message rendering and never makes
     * {@code null} a contract violation.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Mask {

        /**
         * Renderer used to produce the message-safe representation.
         *
         * <p>The default renderer returns a fixed conservative value and reveals
         * no original content, length, or other value details.
         *
         * @return the renderer type used for generated violation messages
         */
        Class<? extends MaskRenderer> renderer() default DefaultMaskRenderer.class;
    }

    /**
     * Requires a character sequence, collection, map, or array value to be non-empty when non-null.
     *
     * <p>Supported values are {@link CharSequence}, {@link java.util.Collection},
     * {@link java.util.Map}, and arrays. A {@code null} object value is ignored
     * and therefore passes. Empty supported values fail.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NotEmpty {

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a character sequence to contain non-whitespace content when non-null.
     *
     * <p>Supported values are {@link CharSequence} instances. A {@code null}
     * object value is ignored and therefore passes. Empty sequences and
     * sequences containing only whitespace fail.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NotBlank {

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a numeric value to be greater than zero.
     *
     * <p>Supported values are primitive numeric values, numeric wrapper types,
     * {@link java.math.BigDecimal}, and {@link java.math.BigInteger}. A
     * {@code null} object value is ignored and therefore passes.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Positive {

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a numeric value to be less than zero.
     *
     * <p>Supported values are primitive numeric values, numeric wrapper types,
     * {@link java.math.BigDecimal}, and {@link java.math.BigInteger}. A
     * {@code null} object value is ignored and therefore passes.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Negative {

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a numeric value to be greater than or equal to zero.
     *
     * <p>Supported values are primitive numeric values, numeric wrapper types,
     * {@link java.math.BigDecimal}, and {@link java.math.BigInteger}. A
     * {@code null} object value is ignored and therefore passes.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NonNegative {

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a numeric value to be less than or equal to zero.
     *
     * <p>Supported values are primitive numeric values, numeric wrapper types,
     * {@link java.math.BigDecimal}, and {@link java.math.BigInteger}. A
     * {@code null} object value is ignored and therefore passes.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NonPositive {

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a numeric value to fall within the configured range.
     *
     * <p>Supported values are primitive numeric values, numeric wrapper types,
     * {@link java.math.BigDecimal}, and {@link java.math.BigInteger}. A
     * {@code null} object value is ignored and therefore passes.
     *
     * <p>Both range endpoints are inclusive by default. Use
     * {@link #minInclusive()} and {@link #maxInclusive()} to make either bound
     * exclusive.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface InRange {

        /**
         * Lower numeric bound.
         *
         * @return the minimum allowed value
         */
        long min();

        /**
         * Upper numeric bound.
         *
         * @return the maximum allowed value
         */
        long max();

        /**
         * Whether the lower bound is inclusive.
         *
         * @return {@code true} when values equal to {@link #min()} pass
         */
        boolean minInclusive() default true;

        /**
         * Whether the upper bound is inclusive.
         *
         * @return {@code true} when values equal to {@link #max()} pass
         */
        boolean maxInclusive() default true;

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a character sequence, collection, map, or array value size to be in range when non-null.
     *
     * <p>Supported values are {@link CharSequence}, {@link java.util.Collection},
     * {@link java.util.Map}, and arrays. A {@code null} object value is ignored
     * and therefore passes. The configured range is inclusive.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Size {

        /**
         * Inclusive minimum length or size.
         *
         * @return the minimum allowed length or size
         */
        int min() default 0;

        /**
         * Inclusive maximum length or size.
         *
         * @return the maximum allowed length or size
         */
        int max() default Integer.MAX_VALUE;

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }

    /**
     * Requires a character sequence to match the configured regular expression when non-null.
     *
     * <p>Supported values are {@link CharSequence} instances. A {@code null}
     * object value is ignored and therefore passes. Pattern evaluation uses
     * {@link java.util.regex.Matcher#matches()}, so the full sequence must match
     * the expression.
     */
    @API(status = API.Status.MAINTAINED)
    @Contract
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Pattern {

        /**
         * Regular expression that the full character sequence must match.
         *
         * @return the Java regular expression
         */
        String regexp();

        /**
         * Custom violation description.
         *
         * <p>When non-empty, this text replaces the generated contract
         * description while preserving location details, exception type, and
         * masking behavior.
         *
         * @return the custom violation description, or an empty string to use
         *     the generated description
         */
        String message() default "";
    }
}
