package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class PositiveContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.isPositive(null));
    }

    @Test
    void positivePrimitiveIntegersPass() {
        assertTrue(ContractRuntime.isPositive(1));
        assertTrue(ContractRuntime.isPositive(Integer.MAX_VALUE));
        assertTrue(ContractRuntime.isPositive(42));
    }

    @Test
    void positiveBoxedIntegersPass() {
        assertTrue(ContractRuntime.isPositive(Integer.valueOf(1)));
        assertTrue(ContractRuntime.isPositive(Integer.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    void zeroAndNegativeIntegersFail() {
        assertFalse(ContractRuntime.isPositive(0));
        assertFalse(ContractRuntime.isPositive(-1));
        assertFalse(ContractRuntime.isPositive(Integer.MIN_VALUE));
    }

    @Test
    void positivePrimitiveLongsPass() {
        assertTrue(ContractRuntime.isPositive(1L));
        assertTrue(ContractRuntime.isPositive(Long.MAX_VALUE));
    }

    @Test
    void positiveBoxedLongsPass() {
        assertTrue(ContractRuntime.isPositive(Long.valueOf(1L)));
        assertTrue(ContractRuntime.isPositive(Long.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void zeroAndNegativeLongsFail() {
        assertFalse(ContractRuntime.isPositive(0L));
        assertFalse(ContractRuntime.isPositive(-1L));
        assertFalse(ContractRuntime.isPositive(Long.MIN_VALUE));
    }

    @Test
    void positiveDoublesPass() {
        assertTrue(ContractRuntime.isPositive(1.0));
        assertTrue(ContractRuntime.isPositive(Double.MAX_VALUE));
        assertTrue(ContractRuntime.isPositive(Double.MIN_VALUE));
        assertTrue(ContractRuntime.isPositive(0.0001));
    }

    @Test
    void negativeZeroDoubleFails() {
        assertFalse(ContractRuntime.isPositive(-0.0));
    }

    @Test
    void zeroDoubleFails() {
        assertFalse(ContractRuntime.isPositive(0.0));
    }

    @Test
    void nanAndInfinityDoublesBehavior() {
        assertFalse(ContractRuntime.isPositive(Double.NaN));
        assertTrue(ContractRuntime.isPositive(Double.POSITIVE_INFINITY));
        assertFalse(ContractRuntime.isPositive(Double.NEGATIVE_INFINITY));
    }

    @Test
    void negativeDoublesFail() {
        assertFalse(ContractRuntime.isPositive(-1.0));
        assertFalse(ContractRuntime.isPositive(-Double.MAX_VALUE));
    }

    @Test
    void positiveFloatsPass() {
        assertTrue(ContractRuntime.isPositive(1.0f));
        assertTrue(ContractRuntime.isPositive(Float.MAX_VALUE));
        assertTrue(ContractRuntime.isPositive(Float.MIN_VALUE));
    }

    @Test
    void negativeZeroFloatFails() {
        assertFalse(ContractRuntime.isPositive(-0.0f));
    }

    @Test
    void zeroFloatFails() {
        assertFalse(ContractRuntime.isPositive(0.0f));
    }

    @Test
    void nanAndInfinityFloatsBehavior() {
        assertFalse(ContractRuntime.isPositive(Float.NaN));
        assertTrue(ContractRuntime.isPositive(Float.POSITIVE_INFINITY));
        assertFalse(ContractRuntime.isPositive(Float.NEGATIVE_INFINITY));
    }

    @Test
    void negativeFloatsFail() {
        assertFalse(ContractRuntime.isPositive(-1.0f));
    }

    @Test
    void positiveBigDecimalsPass() {
        assertTrue(ContractRuntime.isPositive(new BigDecimal("0.0001")));
        assertTrue(ContractRuntime.isPositive(BigDecimal.ONE));
        assertTrue(ContractRuntime.isPositive(new BigDecimal("999999999999999999999")));
    }

    @Test
    void zeroAndNegativeBigDecimalsFail() {
        assertFalse(ContractRuntime.isPositive(BigDecimal.ZERO));
        assertFalse(ContractRuntime.isPositive(new BigDecimal("-0.0001")));
        assertFalse(ContractRuntime.isPositive(new BigDecimal("-999999999999999999999")));
    }

    @Test
    void positiveBigIntegersPass() {
        assertTrue(ContractRuntime.isPositive(BigInteger.ONE));
        assertTrue(ContractRuntime.isPositive(new BigInteger("999999999999999999999")));
    }

    @Test
    void zeroAndNegativeBigIntegersFail() {
        assertFalse(ContractRuntime.isPositive(BigInteger.ZERO));
        assertFalse(ContractRuntime.isPositive(BigInteger.valueOf(-1)));
        assertFalse(ContractRuntime.isPositive(new BigInteger("-999999999999999999999")));
    }

    @Test
    void positiveShortsPass() {
        assertTrue(ContractRuntime.isPositive((short) 1));
        assertTrue(ContractRuntime.isPositive(Short.MAX_VALUE));
    }

    @Test
    void zeroAndNegativeShortsFail() {
        assertFalse(ContractRuntime.isPositive((short) 0));
        assertFalse(ContractRuntime.isPositive(Short.MIN_VALUE));
    }

    @Test
    void positiveBytesPass() {
        assertTrue(ContractRuntime.isPositive((byte) 1));
        assertTrue(ContractRuntime.isPositive(Byte.MAX_VALUE));
    }

    @Test
    void zeroAndNegativeBytesFail() {
        assertFalse(ContractRuntime.isPositive((byte) 0));
        assertFalse(ContractRuntime.isPositive(Byte.MIN_VALUE));
    }

    @Test
    void unsupportedTypesFail() {
        assertFalse(ContractRuntime.isPositive("1"));
        assertFalse(ContractRuntime.isPositive(true));
    }
}
