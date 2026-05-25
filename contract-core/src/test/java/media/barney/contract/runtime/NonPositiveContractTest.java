package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NonPositiveContractTest {

    @Test
    void acceptsNegativeNumbersAndSignedZero() {
        assertTrue(ContractRuntime.isNonPositive(0));
        assertTrue(ContractRuntime.isNonPositive(0.0d));
        assertTrue(ContractRuntime.isNonPositive(-0.0d));
        assertTrue(ContractRuntime.isNonPositive(Long.MIN_VALUE));

        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                -0.0d,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("nonPositiveDouble", Double.class)));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(ContractRuntime.isNonPositive(null));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("nonPositiveDouble", Double.class)));
    }

    @Test
    void rejectsPositiveValuesAndNan() {
        assertFalse(ContractRuntime.isNonPositive(1));
        assertFalse(ContractRuntime.isNonPositive(Double.NaN));
    }
}
