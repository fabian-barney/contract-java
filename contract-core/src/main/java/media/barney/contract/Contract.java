package media.barney.contract;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Namespace and meta-annotation for declarative Java contracts.
 *
 * <p>Built-in contract annotations are nested under this type so a single
 * import gives callers access to declarations such as {@code @Contract.Positive}
 * and {@code @Contract.Pattern}. Custom composed annotations can be marked with
 * {@code @Contract} and may combine built-in annotations.
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface Contract {

    /**
     * Marks a value as confidential for generated violation messages.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Mask {

        Class<? extends MaskRenderer> renderer() default DefaultMaskRenderer.class;
    }

    /**
     * Requires a string, collection, map, or array value to be non-empty when non-null.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NotEmpty {

        String message() default "";
    }

    /**
     * Requires a character sequence to contain non-whitespace content when non-null.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NotBlank {

        String message() default "";
    }

    /**
     * Requires a numeric value to be greater than zero.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Positive {

        String message() default "";
    }

    /**
     * Requires a numeric value to be less than zero.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Negative {

        String message() default "";
    }

    /**
     * Requires a numeric value to be greater than or equal to zero.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NonNegative {

        String message() default "";
    }

    /**
     * Requires a numeric value to be less than or equal to zero.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface NonPositive {

        String message() default "";
    }

    /**
     * Requires a numeric value to fall within the configured range.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface InRange {

        long min();

        long max();

        boolean minInclusive() default true;

        boolean maxInclusive() default true;

        String message() default "";
    }

    /**
     * Requires a string, collection, map, or array value size to be in range when non-null.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Size {

        int min() default 0;

        int max() default Integer.MAX_VALUE;

        String message() default "";
    }

    /**
     * Requires a character sequence to match the configured regular expression when non-null.
     */
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface Pattern {

        String regexp();

        String message() default "";
    }
}
