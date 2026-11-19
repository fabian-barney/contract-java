package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NonPositiveContractTest {

    private static final GeneratedCheck NON_POSITIVE = GeneratedCheck.nonPositive();

    @Test
    void acceptsNegativeNumbersAndSignedZero() {
        assertTrue(NON_POSITIVE.accepts(0));
        assertTrue(NON_POSITIVE.accepts(0.0d));
        assertTrue(NON_POSITIVE.accepts(-0.0d));
        assertTrue(NON_POSITIVE.accepts(Long.MIN_VALUE));

        assertDoesNotThrow(() -> NON_POSITIVE.requireParameter(-0.0d, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(NON_POSITIVE.accepts(null));
        assertDoesNotThrow(() -> NON_POSITIVE.requireParameter(null, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void rejectsPositiveValuesAndNan() {
        assertFalse(NON_POSITIVE.accepts(1));
        assertFalse(NON_POSITIVE.accepts(Double.NaN));
    }
}
