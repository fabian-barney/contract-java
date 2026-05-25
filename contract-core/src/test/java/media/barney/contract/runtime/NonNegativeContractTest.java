package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class NonNegativeContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.isNonNegative(null));
    }

    @Test
    void zeroAndPositivePrimitiveIntegersPass() {
        assertTrue(ContractRuntime.isNonNegative(0));
        assertTrue(ContractRuntime.isNonNegative(1));
        assertTrue(ContractRuntime.isNonNegative(Integer.MAX_VALUE));
    }

    @Test
    void zeroAndPositiveBoxedIntegersPass() {
        assertTrue(ContractRuntime.isNonNegative(Integer.valueOf(0)));
        assertTrue(ContractRuntime.isNonNegative(Integer.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    void negativeIntegersFail() {
        assertFalse(ContractRuntime.isNonNegative(-1));
        assertFalse(ContractRuntime.isNonNegative(Integer.MIN_VALUE));
    }

    @Test
    void zeroAndPositivePrimitiveLongsPass() {
        assertTrue(ContractRuntime.isNonNegative(0L));
        assertTrue(ContractRuntime.isNonNegative(1L));
        assertTrue(ContractRuntime.isNonNegative(Long.MAX_VALUE));
    }

    @Test
    void zeroAndPositiveBoxedLongsPass() {
        assertTrue(ContractRuntime.isNonNegative(Long.valueOf(0L)));
        assertTrue(ContractRuntime.isNonNegative(Long.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void negativeLongsFail() {
        assertFalse(ContractRuntime.isNonNegative(-1L));
        assertFalse(ContractRuntime.isNonNegative(Long.MIN_VALUE));
    }

    @Test
    void zeroAndPositiveDoublesPass() {
        assertTrue(ContractRuntime.isNonNegative(0.0));
        assertTrue(ContractRuntime.isNonNegative(1.0));
        assertTrue(ContractRuntime.isNonNegative(Double.MAX_VALUE));
        assertTrue(ContractRuntime.isNonNegative(Double.MIN_VALUE));
    }

    @Test
    void negativeZeroDoubleFails() {
        assertFalse(ContractRuntime.isNonNegative(-0.0));
    }

    @Test
    void nanAndInfinityDoublesBehavior() {
        // NaN is UNSUPPORTED, so isNonNegative returns false
        assertFalse(ContractRuntime.isNonNegative(Double.NaN));
        
        // Positive infinity >= 0, so it IS non-negative
        assertTrue(ContractRuntime.isNonNegative(Double.POSITIVE_INFINITY));
        
        // Negative infinity < 0, so NOT non-negative
        assertFalse(ContractRuntime.isNonNegative(Double.NEGATIVE_INFINITY));
    }

    @Test
    void negativeDoublesFail() {
        assertFalse(ContractRuntime.isNonNegative(-1.0));
        assertFalse(ContractRuntime.isNonNegative(-Double.MAX_VALUE));
    }

    @Test
    void zeroAndPositiveFloatsPass() {
        assertTrue(ContractRuntime.isNonNegative(0.0f));
        assertTrue(ContractRuntime.isNonNegative(1.0f));
        assertTrue(ContractRuntime.isNonNegative(Float.MAX_VALUE));
    }

    @Test
    void negativeZeroFloatFails() {
        assertFalse(ContractRuntime.isNonNegative(-0.0f));
    }

    @Test
    void nanAndInfinityFloatsBehavior() {
        // NaN is UNSUPPORTED
        assertFalse(ContractRuntime.isNonNegative(Float.NaN));
        
        // Positive infinity IS non-negative
        assertTrue(ContractRuntime.isNonNegative(Float.POSITIVE_INFINITY));
        
        // Negative infinity is NOT non-negative
        assertFalse(ContractRuntime.isNonNegative(Float.NEGATIVE_INFINITY));
    }

    @Test
    void negativeFloatsFail() {
        assertFalse(ContractRuntime.isNonNegative(-1.0f));
    }

    @Test
    void zeroAndPositiveBigDecimalsPass() {
        assertTrue(ContractRuntime.isNonNegative(BigDecimal.ZERO));
        assertTrue(ContractRuntime.isNonNegative(BigDecimal.ONE));
        assertTrue(ContractRuntime.isNonNegative(new BigDecimal("0.0001")));
    }

    @Test
    void negativeBigDecimalsFail() {
        assertFalse(ContractRuntime.isNonNegative(new BigDecimal("-0.0001")));
        assertFalse(ContractRuntime.isNonNegative(BigDecimal.ONE.negate()));
    }

    @Test
    void zeroAndPositiveBigIntegersPass() {
        assertTrue(ContractRuntime.isNonNegative(BigInteger.ZERO));
        assertTrue(ContractRuntime.isNonNegative(BigInteger.ONE));
        assertTrue(ContractRuntime.isNonNegative(new BigInteger("999999999999999999999")));
    }

    @Test
    void negativeBigIntegersFail() {
        assertFalse(ContractRuntime.isNonNegative(BigInteger.ONE.negate()));
        assertFalse(ContractRuntime.isNonNegative(new BigInteger("-999999999999999999999")));
    }

    @Test
    void zeroAndPositiveShortsPass() {
        assertTrue(ContractRuntime.isNonNegative((short) 0));
        assertTrue(ContractRuntime.isNonNegative((short) 1));
        assertTrue(ContractRuntime.isNonNegative(Short.MAX_VALUE));
    }

    @Test
    void negativeShortsFail() {
        assertFalse(ContractRuntime.isNonNegative((short) -1));
        assertFalse(ContractRuntime.isNonNegative(Short.MIN_VALUE));
    }

    @Test
    void zeroAndPositiveBytesPass() {
        assertTrue(ContractRuntime.isNonNegative((byte) 0));
        assertTrue(ContractRuntime.isNonNegative((byte) 1));
        assertTrue(ContractRuntime.isNonNegative(Byte.MAX_VALUE));
    }

    @Test
    void negativeBytesFail() {
        assertFalse(ContractRuntime.isNonNegative((byte) -1));
        assertFalse(ContractRuntime.isNonNegative(Byte.MIN_VALUE));
    }

    @Test
    void unsupportedTypesFail() {
        assertFalse(ContractRuntime.isNonNegative("0"));
        assertFalse(ContractRuntime.isNonNegative(true));
    }
}
