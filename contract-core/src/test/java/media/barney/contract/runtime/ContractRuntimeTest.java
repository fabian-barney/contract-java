package media.barney.contract.runtime;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import media.barney.contract.Contract;
import media.barney.contract.MaskRenderer;
import org.junit.jupiter.api.Test;

class ContractRuntimeTest {

    @Test
    void semanticChecksIgnoreNullValues() {
        assertTrue(ContractRuntime.isNotEmpty(null));
        assertTrue(ContractRuntime.isNotBlank(null));
        assertTrue(ContractRuntime.isPositive(null));
        assertTrue(ContractRuntime.isNegative(null));
        assertTrue(ContractRuntime.isNonNegative(null));
        assertTrue(ContractRuntime.isNonPositive(null));
        assertTrue(ContractRuntime.isInRange(null, 1, 3, true, false));
        assertTrue(ContractRuntime.hasSize(null, 1, 3));
        assertTrue(ContractRuntime.matchesPattern(null, "[0-9]+"));

        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null, "com.example.UserService.findUser", "limit", parameterAnnotations("positive", Integer.class)));
    }

    @Test
    void contentPredicatesSupportStringsCollectionsMapsAndArrays() {
        assertTrue(ContractRuntime.isNotEmpty(new StringBuilder("abc")));
        assertTrue(ContractRuntime.isNotEmpty(List.of("a")));
        assertTrue(ContractRuntime.isNotEmpty(Map.of("a", "b")));
        assertTrue(ContractRuntime.isNotEmpty(new int[] {1}));

        assertFalse(ContractRuntime.isNotEmpty(""));
        assertFalse(ContractRuntime.isNotEmpty(List.of()));
        assertFalse(ContractRuntime.isNotBlank(" \t\n"));
    }

    @Test
    void numericPredicatesSupportBigDecimalsBigIntegersAndRanges() {
        assertTrue(ContractRuntime.isPositive(new BigDecimal("1.5")));
        assertTrue(ContractRuntime.isNegative(BigInteger.valueOf(-1L)));
        assertTrue(ContractRuntime.isNonNegative(0));
        assertTrue(ContractRuntime.isNonPositive(0L));

        assertTrue(ContractRuntime.isInRange(new BigDecimal("2.5"), 1, 3, true, false));
        assertTrue(ContractRuntime.isInRange(BigInteger.TWO, 1, 3, true, false));
        assertTrue(ContractRuntime.isInRange(2.5d, 1, 3, true, false));
        assertTrue(ContractRuntime.isInRange(2.5f, 1, 3, true, false));
        assertTrue(ContractRuntime.isInRange(2L, 1, 3, true, false));
        assertFalse(ContractRuntime.isInRange(3, 1, 3, true, false));
        assertFalse(ContractRuntime.isInRange(Double.NaN, 1, 3, true, false));
        assertFalse(ContractRuntime.isInRange("2", 1, 3, true, false));
        assertFalse(ContractRuntime.isPositive(Double.NaN));
        assertFalse(ContractRuntime.isPositive("1"));
    }

    @Test
    void sizeChecksSupportCharSequencesCollectionsMapsAndArrays() {
        assertTrue(ContractRuntime.hasSize("ab", 1, 2));
        assertTrue(ContractRuntime.hasSize(new StringBuilder("ab"), 1, 2));
        assertTrue(ContractRuntime.hasSize(List.of("a", "b"), 1, 2));
        assertTrue(ContractRuntime.hasSize(Map.of("a", 1), 1, 2));
        assertTrue(ContractRuntime.hasSize(new long[] {1L, 2L}, 1, 2));

        assertFalse(ContractRuntime.hasSize("abc", 1, 2));
        assertFalse(ContractRuntime.hasSize(42, -1, 10));
    }

    @Test
    void parameterViolationIncludesLocationContractAndQuotedValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        "",
                        "com.example.UserService.findUser",
                        "tenant",
                        parameterAnnotations("tenant", String.class)));

        assertEquals(
                "Parameter 'tenant' of method 'com.example.UserService.findUser' "
                        + "must have size within [1, 32], but was: \"\"",
                exception.getMessage());
    }

    @Test
    void returnViolationUsesIllegalStateExceptionAndDefaultMasking() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ContractRuntime.requireReturn(
                        "", "com.example.TokenService.issue", methodAnnotations("maskedNotBlank")));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must not be blank, but was: [MASKED]",
                exception.getMessage());
    }

    @Test
    void customComposedContractUsesCustomMessage() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        0L,
                        "com.example.UserService.deleteUser",
                        "userId",
                        parameterAnnotations("validId", Long.class)));

        assertEquals(
                "Parameter 'userId' of method 'com.example.UserService.deleteUser': "
                        + "must be a valid ID, but was: 0",
                exception.getMessage());
    }

    @Test
    void customMaskRendererIsAppliedWithoutRawValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        "secret-token",
                        "com.example.AccountService.login",
                        "password",
                        parameterAnnotations("sensitivePattern", String.class)));

        assertEquals(
                "Parameter 'password' of method 'com.example.AccountService.login' "
                        + "must match the required pattern, but was: [text]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("secret-token"));
    }

    @Test
    void brokenMaskRendererFallsBackConservatively() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ContractRuntime.requireReturn(
                        "raw-secret", "com.example.TokenService.issue", methodAnnotations("throwingMask")));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must match the required pattern, but was: [MASKED]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("raw-secret"));
    }

    @Test
    void nonFatalMaskRendererErrorsFallBackConservatively() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ContractRuntime.requireReturn(
                        "raw-secret", "com.example.TokenService.issue", methodAnnotations("linkageMask")));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must match the required pattern, but was: [MASKED]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("raw-secret"));
    }

    @Test
    void patternChecksReuseCompiledPatternBehavior() {
        assertTrue(ContractRuntime.matchesPattern("USR-42", "USR-[0-9]+"));
        assertFalse(ContractRuntime.matchesPattern("TEN-42", "USR-[0-9]+"));
    }

    @Test
    void customBuiltInMessageKeepsLocationAndValueRendering() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        BigDecimal.ZERO,
                        "com.example.AccountService.transfer",
                        "amount",
                        parameterAnnotations("amount", BigDecimal.class)));

        assertEquals(
                "Parameter 'amount' of method 'com.example.AccountService.transfer': "
                        + "transfer amount must be positive, but was: 0",
                exception.getMessage());
    }

    @Test
    void valueRenderingQuotesEscapesAndFormatsArrays() {
        assertEquals("\"a\\n\\\"\\\\\\t\\r\"", ContractRuntime.renderValue("a\n\"\\\t\r", null));
        assertEquals("'\\n'", ContractRuntime.renderValue('\n', null));
        assertEquals("[1, 2]", ContractRuntime.renderValue(new int[] {1, 2}, null));
        assertEquals("[a, [b]]", ContractRuntime.renderValue(new Object[] {"a", new String[] {"b"}}, null));
    }

    private static Annotation[] parameterAnnotations(String methodName, Class<?> parameterType) {
        try {
            return Fixture.class.getDeclaredMethod(methodName, parameterType).getParameterAnnotations()[0];
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Annotation[] methodAnnotations(String methodName) {
        try {
            return Fixture.class.getDeclaredMethod(methodName).getAnnotations();
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings({"unused", "PMD.UnusedPrivateMethod"})
    private static final class Fixture {

        void positive(@Contract.Positive Integer limit) {}

        void tenant(@Contract.Size(min = 1, max = 32) String tenant) {}

        void validId(@ValidId Long userId) {}

        void sensitivePattern(@SensitivePattern String password) {}

        void amount(@Contract.Positive(message = "transfer amount must be positive") BigDecimal amount) {}

        @Contract.Mask
        @Contract.NotBlank String maskedNotBlank() {
            return "";
        }

        @Contract.Mask(renderer = ThrowingMaskRenderer.class)
        @Contract.Pattern(regexp = "[0-9]+") String throwingMask() {
            return "";
        }

        @Contract.Mask(renderer = LinkageErrorMaskRenderer.class)
        @Contract.Pattern(regexp = "[0-9]+") String linkageMask() {
            return "";
        }
    }

    @Contract
    @Contract.Positive @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    private @interface ValidId {

        String message() default "must be a valid ID";
    }

    @Contract
    @Contract.Mask(renderer = TextMaskRenderer.class)
    @Contract.Pattern(regexp = "[0-9]+") @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    private @interface SensitivePattern {}

    public static final class TextMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            return "[text]";
        }
    }

    public static final class ThrowingMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new IllegalStateException("renderer failed");
        }
    }

    public static final class LinkageErrorMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new LinkageError("renderer linkage failed");
        }
    }
}
