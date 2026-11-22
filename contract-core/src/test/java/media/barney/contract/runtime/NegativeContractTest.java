package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NegativeContractTest {

    private static final GeneratedCheck NEGATIVE = GeneratedCheck.negative();

    @Test
    void supportsPrimitiveAndBoxedNumbers() {
        assertTrue(NEGATIVE.accepts(-1));
        assertTrue(NEGATIVE.accepts(Long.MIN_VALUE));
        assertDoesNotThrow(() -> NEGATIVE.requireParameter(-1.0d, "com.example.UserService.deleteUser", "limit"));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(NEGATIVE.accepts(null));
        assertDoesNotThrow(() -> NEGATIVE.requireParameter(null, "com.example.UserService.deleteUser", "limit"));
    }

    @Test
    void rejectsZeroPositiveNanAndNegativeZero() {
        assertFalse(NEGATIVE.accepts(0));
        assertFalse(NEGATIVE.accepts(1));
        assertFalse(NEGATIVE.accepts(Double.NaN));
        assertFalse(NEGATIVE.accepts(-0.0d));
    }
}
