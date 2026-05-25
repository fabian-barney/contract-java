package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PositiveContractTest {

    @Test
    void supportsPrimitiveAndBoxedNumbers() {
        assertTrue(ContractRuntime.isPositive(1));
        assertTrue(ContractRuntime.isPositive(Long.MAX_VALUE));
        assertTrue(ContractRuntime.isPositive(new BigDecimal("1.5")));

        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                1,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("positiveInt", int.class)));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                1,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("positiveInteger", Integer.class)));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(ContractRuntime.isPositive(null));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("positiveInteger", Integer.class)));
    }

    @Test
    void rejectsZeroNegativeNanAndNegativeZero() {
        assertFalse(ContractRuntime.isPositive(0));
        assertFalse(ContractRuntime.isPositive(-1));
        assertFalse(ContractRuntime.isPositive(Double.NaN));
        assertFalse(ContractRuntime.isPositive(-0.0d));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        0,
                        "com.example.UserService.findUser",
                        "limit",
                        BuiltInContractTestSupport.parameterAnnotations("positiveInt", int.class)));

        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    void customMessagesKeepValueRendering() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        BigDecimal.ZERO,
                        "com.example.AccountService.transfer",
                        "amount",
                        BuiltInContractTestSupport.parameterAnnotations("amount", BigDecimal.class)));

        assertEquals(
                "Parameter 'amount' of method 'com.example.AccountService.transfer': "
                        + "transfer amount must be positive, but was: 0",
                exception.getMessage());
    }
}
