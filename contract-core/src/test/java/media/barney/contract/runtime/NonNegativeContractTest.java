package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NonNegativeContractTest {

    private static final GeneratedCheck NON_NEGATIVE = GeneratedCheck.nonNegative();

    @Test
    void acceptsPositiveNumbersAndSignedZero() {
        assertTrue(NON_NEGATIVE.accepts(0));
        assertTrue(NON_NEGATIVE.accepts(0.0d));
        assertTrue(NON_NEGATIVE.accepts(-0.0d));
        assertTrue(NON_NEGATIVE.accepts(Long.MAX_VALUE));

        assertDoesNotThrow(() -> NON_NEGATIVE.requireParameter(-0.0d, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(NON_NEGATIVE.accepts(null));
        assertDoesNotThrow(() -> NON_NEGATIVE.requireParameter(null, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void rejectsNegativeValuesAndNan() {
        assertFalse(NON_NEGATIVE.accepts(-1));
        assertFalse(NON_NEGATIVE.accepts(Double.NaN));
    }
}
