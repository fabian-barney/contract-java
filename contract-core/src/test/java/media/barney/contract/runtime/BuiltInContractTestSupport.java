package media.barney.contract.runtime;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import media.barney.contract.Contract;
import media.barney.contract.MaskRenderer;

final class BuiltInContractTestSupport {

    private BuiltInContractTestSupport() {}

    static Annotation[] parameterAnnotations(String methodName, Class<?> parameterType) {
        try {
            return Fixture.class.getDeclaredMethod(methodName, parameterType).getParameterAnnotations()[0];
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    static Annotation[] methodAnnotations(String methodName) {
        try {
            return Fixture.class.getDeclaredMethod(methodName).getAnnotations();
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings({"unused", "PMD.UnusedPrivateMethod"})
    private static final class Fixture {

        void notEmptyString(@Contract.NotEmpty String value) {}

        void notBlankString(@Contract.NotBlank String value) {}

        void positiveInt(@Contract.Positive int value) {}

        void positiveInteger(@Contract.Positive Integer value) {}

        void negativeDouble(@Contract.Negative double value) {}

        void negativeBoxed(@Contract.Negative Double value) {}

        void nonNegativeDouble(@Contract.NonNegative Double value) {}

        void nonPositiveDouble(@Contract.NonPositive Double value) {}

        void rangeExclusiveUpper(@Contract.InRange(min = 0, max = 2, maxInclusive = false) Double value) {}

        void sizeString(@Contract.Size(min = 1, max = 2) String value) {}

        void patternIgnoreCase(@Contract.Pattern(regexp = "(?i)usr-[0-9]+") String value) {}

        void sensitivePattern(@SensitivePattern String password) {}

        void validId(@ValidId Long userId) {}

        void amount(@Contract.Positive(message = "transfer amount must be positive") BigDecimal amount) {}

        @Contract.Mask
        @Contract.NotBlank
        String maskedNotBlank() {
            return "";
        }

        @Contract.Mask(renderer = ThrowingMaskRenderer.class)
        @Contract.Pattern(regexp = "[0-9]+")
        String throwingMask() {
            return "";
        }

        @Contract.Mask(renderer = LinkageErrorMaskRenderer.class)
        @Contract.Pattern(regexp = "[0-9]+")
        String linkageMask() {
            return "";
        }
    }

    @Contract
    @Contract.Positive
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    private @interface ValidId {

        String message() default "must be a valid ID";
    }

    @Contract
    @Contract.Mask(renderer = TextMaskRenderer.class)
    @Contract.Pattern(regexp = "[0-9]+")
    @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    private @interface SensitivePattern {}

    static final class TextMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            return "[text]";
        }
    }

    static final class ThrowingMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new IllegalStateException("renderer failed");
        }
    }

    static final class LinkageErrorMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new LinkageError("renderer linkage failed");
        }
    }
}
