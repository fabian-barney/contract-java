package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class NegativeContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.isNegative(null));
    }

    @Test
    void negativePrimitiveIntegersPass() {
        assertTrue(ContractRuntime.isNegative(-1));
        assertTrue(ContractRuntime.isNegative(Integer.MIN_VALUE));
        assertTrue(ContractRuntime.isNegative(-42));
    }

    @Test
    void negativeBoxedIntegersPass() {
        assertTrue(ContractRuntime.isNegative(Integer.valueOf(-1)));
        assertTrue(ContractRuntime.isNegative(Integer.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void zeroAndPositiveIntegersFail() {
        assertFalse(ContractRuntime.isNegative(0));
        assertFalse(ContractRuntime.isNegative(1));
        assertFalse(ContractRuntime.isNegative(Integer.MAX_VALUE));
    }

    @Test
    void negativePrimitiveLongsPass() {
        assertTrue(ContractRuntime.isNegative(-1L));
        assertTrue(ContractRuntime.isNegative(Long.MIN_VALUE));
    }

    @Test
    void negativeBoxedLongsPass() {
        assertTrue(ContractRuntime.isNegative(Long.valueOf(-1L)));
        assertTrue(ContractRuntime.isNegative(Long.valueOf(Long.MIN_VALUE)));
    }

    @Test
    void zeroAndPositiveLongsFail() {
        assertFalse(ContractRuntime.isNegative(0L));
        assertFalse(ContractRuntime.isNegative(1L));
        assertFalse(ContractRuntime.isNegative(Long.MAX_VALUE));
    }

    @Test
    void negativeDoublesPass() {
        assertTrue(ContractRuntime.isNegative(-1.0));
        assertTrue(ContractRuntime.isNegative(-Double.MAX_VALUE));
        assertTrue(ContractRuntime.isNegative(-0.0001));
    }

    @Test
    void negativeZeroDoubleIsConsideredNegative() {
        // Java's Double.compare(-0.0, 0.0) returns -1, so -0.0 is considered negative
        assertTrue(ContractRuntime.isNegative(-0.0));
    }

    @Test
    void zeroDoubleIsNotNegative() {
        assertFalse(ContractRuntime.isNegative(0.0));
    }

    @Test
    void nanDoubleIsNotNegative() {
        // NaN is UNSUPPORTED, so isNegative returns false
        assertFalse(ContractRuntime.isNegative(Double.NaN));
    }

    @Test
    void infinityDoublesBehavior() {
        // Positive infinity is greater than 0, so not negative
        assertFalse(ContractRuntime.isNegative(Double.POSITIVE_INFINITY));
        // Negative infinity is less than 0, so it IS negative
        assertTrue(ContractRuntime.isNegative(Double.NEGATIVE_INFINITY));
    }

    @Test
    void positiveDoublesFail() {
        assertFalse(ContractRuntime.isNegative(1.0));
        assertFalse(ContractRuntime.isNegative(Double.MAX_VALUE));
    }

    @Test
    void negativeFloatsPass() {
        assertTrue(ContractRuntime.isNegative(-1.0f));
        assertTrue(ContractRuntime.isNegative(-Float.MAX_VALUE));
    }

    @Test
    void negativeZeroFloatIsConsideredNegative() {
        // Java's comparison treats -0.0f as negative
        assertTrue(ContractRuntime.isNegative(-0.0f));
    }

    @Test
    void zeroFloatIsNotNegative() {
        assertFalse(ContractRuntime.isNegative(0.0f));
    }

    @Test
    void nanFloatIsNotNegative() {
        assertFalse(ContractRuntime.isNegative(Float.NaN));
    }

    @Test
    void infinityFloatsBehavior() {
        assertFalse(ContractRuntime.isNegative(Float.POSITIVE_INFINITY));
        assertTrue(ContractRuntime.isNegative(Float.NEGATIVE_INFINITY));
    }

    @Test
    void positiveFloatsFail() {
        assertFalse(ContractRuntime.isNegative(1.0f));
    }

    @Test
    void negativeBigDecimalsPass() {
        assertTrue(ContractRuntime.isNegative(new BigDecimal("-0.0001")));
        assertTrue(ContractRuntime.isNegative(BigDecimal.ONE.negate()));
        assertTrue(ContractRuntime.isNegative(new BigDecimal("-999999999999999999999")));
    }

    @Test
    void zeroAndPositiveBigDecimalsFail() {
        assertFalse(ContractRuntime.isNegative(BigDecimal.ZERO));
        assertFalse(ContractRuntime.isNegative(BigDecimal.ONE));
        assertFalse(ContractRuntime.isNegative(new BigDecimal("0.0001")));
    }

    @Test
    void negativeBigIntegersPass() {
        assertTrue(ContractRuntime.isNegative(BigInteger.ONE.negate()));
        assertTrue(ContractRuntime.isNegative(new BigInteger("-999999999999999999999")));
    }

    @Test
    void zeroAndPositiveBigIntegersFail() {
        assertFalse(ContractRuntime.isNegative(BigInteger.ZERO));
        assertFalse(ContractRuntime.isNegative(BigInteger.ONE));
        assertFalse(ContractRuntime.isNegative(new BigInteger("999999999999999999999")));
    }

    @Test
    void negativeShortsPass() {
        assertTrue(ContractRuntime.isNegative((short) -1));
        assertTrue(ContractRuntime.isNegative(Short.MIN_VALUE));
    }

    @Test
    void zeroAndPositiveShortsFail() {
        assertFalse(ContractRuntime.isNegative((short) 0));
        assertFalse(ContractRuntime.isNegative(Short.MAX_VALUE));
    }

    @Test
    void negativeBytesPass() {
        assertTrue(ContractRuntime.isNegative((byte) -1));
        assertTrue(ContractRuntime.isNegative(Byte.MIN_VALUE));
    }

    @Test
    void zeroAndPositiveBytesFail() {
        assertFalse(ContractRuntime.isNegative((byte) 0));
        assertFalse(ContractRuntime.isNegative(Byte.MAX_VALUE));
    }

    @Test
    void unsupportedTypesFail() {
        assertFalse(ContractRuntime.isNegative("-1"));
        assertFalse(ContractRuntime.isNegative(false));
    }
}
