package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class InRangeContractTest {

    private static final GeneratedCheck EXCLUSIVE_UPPER = GeneratedCheck.inRange(0, 2, true, false);

    @Test
    void supportsIntegralDecimalAndFloatingPointValues() {
        assertTrue(EXCLUSIVE_UPPER.accepts(BigDecimal.ONE));
        assertTrue(EXCLUSIVE_UPPER.accepts(BigInteger.ONE));
        assertTrue(EXCLUSIVE_UPPER.accepts(1.5d));
        assertTrue(EXCLUSIVE_UPPER.accepts(1.5f));
        assertTrue(GeneratedCheck.inRange(Long.MAX_VALUE - 1, Long.MAX_VALUE, false, true)
                .accepts(Long.MAX_VALUE));
    }

    @Test
    void honorsInclusiveAndExclusiveBoundsForSignedZero() {
        assertTrue(EXCLUSIVE_UPPER.accepts(-0.0d));
        assertTrue(EXCLUSIVE_UPPER.accepts(0.0d));
        assertFalse(GeneratedCheck.inRange(0, 2, false, false).accepts(-0.0d));
        assertFalse(EXCLUSIVE_UPPER.accepts(2.0d));

        assertDoesNotThrow(() -> EXCLUSIVE_UPPER.requireParameter(-0.0d, "com.example.UserService.findUser", "limit"));
    }

    @Test
    void rejectsNanAndUnsupportedValues() {
        assertFalse(EXCLUSIVE_UPPER.accepts(Double.NaN));
        assertFalse(EXCLUSIVE_UPPER.accepts("1"));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(EXCLUSIVE_UPPER.accepts(null));
        assertDoesNotThrow(() -> EXCLUSIVE_UPPER.requireParameter(null, "com.example.UserService.findUser", "limit"));
    }
}
