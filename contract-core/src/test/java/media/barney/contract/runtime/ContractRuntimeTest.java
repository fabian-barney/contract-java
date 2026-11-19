package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import media.barney.contract.DefaultMaskRenderer;
import media.barney.contract.MaskRenderer;
import org.junit.jupiter.api.Test;

class ContractRuntimeTest {

    @Test
    void semanticChecksIgnoreNullValues() {
        assertTrue(GeneratedCheck.notEmpty().accepts(null));
        assertTrue(GeneratedCheck.notBlank().accepts(null));
        assertTrue(GeneratedCheck.positive().accepts(null));
        assertTrue(GeneratedCheck.negative().accepts(null));
        assertTrue(GeneratedCheck.nonNegative().accepts(null));
        assertTrue(GeneratedCheck.nonPositive().accepts(null));
        assertTrue(GeneratedCheck.inRange(1, 3, true, false).accepts(null));
        assertTrue(GeneratedCheck.size(1, 3).accepts(null));
        assertTrue(GeneratedCheck.pattern("[0-9]+").accepts(null));

        assertDoesNotThrow(
                () -> GeneratedCheck.positive().requireParameter(null, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void contentPredicatesSupportStringsCollectionsMapsAndArrays() {
        assertTrue(GeneratedCheck.notEmpty().accepts(new StringBuilder("abc")));
        assertTrue(GeneratedCheck.notEmpty().accepts(List.of("a")));
        assertTrue(GeneratedCheck.notEmpty().accepts(Map.of("a", "b")));
        assertTrue(GeneratedCheck.notEmpty().accepts(new int[] {1}));

        assertFalse(GeneratedCheck.notEmpty().accepts(""));
        assertFalse(GeneratedCheck.notEmpty().accepts(List.of()));
        assertFalse(GeneratedCheck.notBlank().accepts(" \t\n"));
    }

    @Test
    void numericPredicatesSupportBigDecimalsBigIntegersAndRanges() {
        assertTrue(GeneratedCheck.positive().accepts(new BigDecimal("1.5")));
        assertTrue(GeneratedCheck.negative().accepts(BigInteger.valueOf(-1L)));
        assertTrue(GeneratedCheck.nonNegative().accepts(0));
        assertTrue(GeneratedCheck.nonPositive().accepts(0L));

        assertTrue(GeneratedCheck.inRange(1, 3, true, false).accepts(new BigDecimal("2.5")));
        assertTrue(GeneratedCheck.inRange(1, 3, true, false).accepts(BigInteger.TWO));
        assertTrue(GeneratedCheck.inRange(1, 3, true, false).accepts(2.5d));
        assertTrue(GeneratedCheck.inRange(1, 3, true, false).accepts(2.5f));
        assertTrue(GeneratedCheck.inRange(1, 3, true, false).accepts(2L));
        assertFalse(GeneratedCheck.inRange(1, 3, true, false).accepts(3));
        assertFalse(GeneratedCheck.inRange(1, 3, true, false).accepts(Double.NaN));
        assertFalse(GeneratedCheck.inRange(1, 3, true, false).accepts("2"));
        assertFalse(GeneratedCheck.positive().accepts(Double.NaN));
        assertFalse(GeneratedCheck.positive().accepts("1"));
    }

    @Test
    void primitiveBridgesUseDedicatedNumericValidationPaths() {
        assertDoesNotThrow(() -> {
            requireLongParameter(1L, RuntimeContract.POSITIVE);
            requireLongParameter(-1L, RuntimeContract.NEGATIVE);
            requireLongParameter(0L, RuntimeContract.NON_NEGATIVE);
            requireLongParameter(0L, RuntimeContract.NON_POSITIVE);
            requireLongParameter(1L, RuntimeContract.IN_RANGE);

            requireDoubleParameter(1.0d, RuntimeContract.POSITIVE);
            requireDoubleParameter(-1.0d, RuntimeContract.NEGATIVE);
            requireDoubleParameter(0.0d, RuntimeContract.NON_NEGATIVE);
            requireDoubleParameter(0.0d, RuntimeContract.NON_POSITIVE);
            requireDoubleParameter(1.0d, RuntimeContract.IN_RANGE);
        });

        assertThrows(IllegalArgumentException.class, () -> requireLongParameter(1L, RuntimeContract.NOT_EMPTY));
        assertThrows(IllegalArgumentException.class, () -> requireDoubleParameter(1.0d, RuntimeContract.NOT_EMPTY));

        assertThrows(IllegalArgumentException.class, () -> requireLongParameter(0L, RuntimeContract.POSITIVE));
        assertThrows(IllegalArgumentException.class, () -> requireLongParameter(0L, RuntimeContract.NEGATIVE));
        assertThrows(IllegalArgumentException.class, () -> requireLongParameter(-1L, RuntimeContract.NON_NEGATIVE));
        assertThrows(IllegalArgumentException.class, () -> requireLongParameter(1L, RuntimeContract.NON_POSITIVE));
        assertThrows(IllegalArgumentException.class, () -> requireLongParameter(3L, RuntimeContract.IN_RANGE));

        assertThrows(IllegalArgumentException.class, () -> requireDoubleParameter(0.0d, RuntimeContract.POSITIVE));
        assertThrows(IllegalArgumentException.class, () -> requireDoubleParameter(0.0d, RuntimeContract.NEGATIVE));
        assertThrows(IllegalArgumentException.class, () -> requireDoubleParameter(-1.0d, RuntimeContract.NON_NEGATIVE));
        assertThrows(IllegalArgumentException.class, () -> requireDoubleParameter(1.0d, RuntimeContract.NON_POSITIVE));
        assertThrows(IllegalArgumentException.class, () -> requireDoubleParameter(3.0d, RuntimeContract.IN_RANGE));
    }

    @Test
    void sizeChecksSupportCharSequencesCollectionsMapsAndArrays() {
        assertTrue(GeneratedCheck.size(1, 2).accepts("ab"));
        assertTrue(GeneratedCheck.size(1, 2).accepts(new StringBuilder("ab")));
        assertTrue(GeneratedCheck.size(1, 2).accepts(List.of("a", "b")));
        assertTrue(GeneratedCheck.size(1, 2).accepts(Map.of("a", 1)));
        assertTrue(GeneratedCheck.size(1, 2).accepts(new long[] {1L, 2L}));

        assertFalse(GeneratedCheck.size(1, 2).accepts("abc"));
        assertFalse(GeneratedCheck.size(-1, 10).accepts(42));
    }

    @Test
    void parameterViolationIncludesLocationContractAndQuotedValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> GeneratedCheck.size(1, 32).requireParameter("", "com.example.UserService.findUser", "tenant"));

        assertEquals(
                "Parameter 'tenant' of method 'com.example.UserService.findUser' "
                        + "must have size within [1, 32], but was: \"\"",
                exception.getMessage());
    }

    @Test
    void returnViolationUsesIllegalStateExceptionAndDefaultMasking() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> GeneratedCheck.notBlank()
                        .masked(DefaultMaskRenderer.class)
                        .requireReturn("", "com.example.TokenService.issue"));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must not be blank, but was: [MASKED]",
                exception.getMessage());
    }

    @Test
    void customDescriptionFollowsLocationAfterColon() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> GeneratedCheck.positive()
                        .withMessage("must be a valid ID")
                        .requireParameter(0L, "com.example.UserService.deleteUser", "userId"));

        assertEquals(
                "Parameter 'userId' of method 'com.example.UserService.deleteUser': "
                        + "must be a valid ID, but was: 0",
                exception.getMessage());
    }

    @Test
    void customMaskRendererIsAppliedWithoutRawValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> GeneratedCheck.pattern("[0-9]+")
                        .masked(TextMaskRenderer.class)
                        .requireParameter("secret-token", "com.example.AccountService.login", "password"));

        assertEquals(
                "Parameter 'password' of method 'com.example.AccountService.login' "
                        + "must match the required pattern, but was: [text]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("secret-token"));
    }

    @Test
    void customMaskRendererIsAppliedToReturnValues() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> GeneratedCheck.pattern("[0-9]+")
                        .masked(TextMaskRenderer.class)
                        .requireReturn("secret-token", "com.example.TokenService.issue"));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must match the required pattern, but was: [text]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("secret-token"));
    }

    @Test
    void brokenMaskRendererFallsBackConservatively() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> GeneratedCheck.pattern("[0-9]+")
                        .masked(ThrowingMaskRenderer.class)
                        .requireReturn("raw-secret", "com.example.TokenService.issue"));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must match the required pattern, but was: [MASKED]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("raw-secret"));
    }

    @Test
    void maskRendererErrorsPropagate() {
        LinkageError exception = assertThrows(
                LinkageError.class,
                () -> GeneratedCheck.pattern("[0-9]+")
                        .masked(LinkageErrorMaskRenderer.class)
                        .requireReturn("raw-secret", "com.example.TokenService.issue"));

        assertEquals("renderer linkage failed", exception.getMessage());
    }

    @Test
    void patternChecksReuseCompiledPatternBehavior() {
        assertTrue(GeneratedCheck.pattern("USR-[0-9]+").accepts("USR-42"));
        assertFalse(GeneratedCheck.pattern("USR-[0-9]+").accepts("TEN-42"));
    }

    @Test
    void customBuiltInMessageKeepsLocationAndValueRendering() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> GeneratedCheck.positive()
                        .withMessage("transfer amount must be positive")
                        .requireParameter(BigDecimal.ZERO, "com.example.AccountService.transfer", "amount"));

        assertEquals(
                "Parameter 'amount' of method 'com.example.AccountService.transfer': "
                        + "transfer amount must be positive, but was: 0",
                exception.getMessage());
    }

    @Test
    void returnBridgeReturnsOriginalValueWhenContractPasses() {
        String value = "USR-42";

        assertSame(value, GeneratedCheck.pattern("USR-[0-9]+").requireReturn(value, "com.example.UserService.find"));
    }

    private static void requireLongParameter(long value, RuntimeContract contract) {
        ContractRuntime.requireParameterValue(
                value,
                "com.example.Service.method",
                "value",
                contract,
                "must satisfy the contract",
                false,
                null,
                -2L,
                2L,
                true,
                true,
                0,
                0,
                null);
    }

    private static void requireDoubleParameter(double value, RuntimeContract contract) {
        ContractRuntime.requireParameterValue(
                value,
                "com.example.Service.method",
                "value",
                contract,
                "must satisfy the contract",
                false,
                null,
                -2L,
                2L,
                true,
                true,
                0,
                0,
                null);
    }

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
