package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NegativeContractTest {

    @Test
    void supportsPrimitiveAndBoxedNumbers() {
        assertTrue(ContractRuntime.isNegative(-1));
        assertTrue(ContractRuntime.isNegative(Long.MIN_VALUE));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                -1.0d,
                "com.example.UserService.deleteUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("negativeDouble", double.class)));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(ContractRuntime.isNegative(null));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.deleteUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("negativeBoxed", Double.class)));
    }

    @Test
    void rejectsZeroPositiveNanAndNegativeZero() {
        assertFalse(ContractRuntime.isNegative(0));
        assertFalse(ContractRuntime.isNegative(1));
        assertFalse(ContractRuntime.isNegative(Double.NaN));
        assertFalse(ContractRuntime.isNegative(-0.0d));
    }
}
