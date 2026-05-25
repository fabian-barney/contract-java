package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NonNegativeContractTest {

    @Test
    void acceptsPositiveNumbersAndSignedZero() {
        assertTrue(ContractRuntime.isNonNegative(0));
        assertTrue(ContractRuntime.isNonNegative(0.0d));
        assertTrue(ContractRuntime.isNonNegative(-0.0d));
        assertTrue(ContractRuntime.isNonNegative(Long.MAX_VALUE));

        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                -0.0d,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("nonNegativeDouble", Double.class)));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(ContractRuntime.isNonNegative(null));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("nonNegativeDouble", Double.class)));
    }

    @Test
    void rejectsNegativeValuesAndNan() {
        assertFalse(ContractRuntime.isNonNegative(-1));
        assertFalse(ContractRuntime.isNonNegative(Double.NaN));
    }
}
