package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PositiveContractTest {

    private static final GeneratedCheck POSITIVE = GeneratedCheck.positive();

    @Test
    void supportsPrimitiveAndBoxedNumbers() {
        assertTrue(POSITIVE.accepts(1));
        assertTrue(POSITIVE.accepts(Long.MAX_VALUE));
        assertTrue(POSITIVE.accepts(1.5f));
        assertTrue(POSITIVE.accepts(new BigDecimal("1.5")));

        assertDoesNotThrow(() -> POSITIVE.requireParameter(1, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(POSITIVE.accepts(null));
        assertDoesNotThrow(() -> POSITIVE.requireParameter(null, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void rejectsZeroNegativeNanAndNegativeZero() {
        assertFalse(POSITIVE.accepts(0));
        assertFalse(POSITIVE.accepts(-1));
        assertFalse(POSITIVE.accepts(Double.NaN));
        assertFalse(POSITIVE.accepts(-0.0d));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> POSITIVE.requireParameter(0, "com.example.UserService.findUser", "limit"));

        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    void customMessagesKeepValueRendering() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> POSITIVE.withMessage("transfer amount must be positive")
                        .requireParameter(BigDecimal.ZERO, "com.example.AccountService.transfer", "amount"));

        assertEquals(
                "Parameter 'amount' of method 'com.example.AccountService.transfer': "
                        + "transfer amount must be positive, but was: 0",
                exception.getMessage());
    }
}
