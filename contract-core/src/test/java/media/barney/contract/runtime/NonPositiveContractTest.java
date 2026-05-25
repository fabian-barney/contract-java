package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class NonPositiveContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.isNonPositive(null));
    }

    @Test
    void zeroAndNegativePrimitiveIntegersPass() {
        assertTrue(ContractRuntime.isNonPositive(0));
        assertTrue(ContractRuntime.isNonPositive(-1));
        assertTrue(ContractRuntime.isNonPositive(Integer.MIN_VALUE));
    }

    @Test
    void zeroAndNegativeBoxedIntegersPass() {
        assertTrue(ContractRuntime.isNonPositive(Integer.valueOf(0)));
        assertTrue(ContractRuntime.isNonPositive(Integer.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void positiveIntegersFail() {
        assertFalse(ContractRuntime.isNonPositive(1));
        assertFalse(ContractRuntime.isNonPositive(Integer.MAX_VALUE));
    }

    @Test
    void zeroAndNegativePrimitiveLongsPass() {
        assertTrue(ContractRuntime.isNonPositive(0L));
        assertTrue(ContractRuntime.isNonPositive(-1L));
        assertTrue(ContractRuntime.isNonPositive(Long.MIN_VALUE));
    }

    @Test
    void zeroAndNegativeBoxedLongsPass() {
        assertTrue(ContractRuntime.isNonPositive(Long.valueOf(0L)));
        assertTrue(ContractRuntime.isNonPositive(Long.valueOf(Long.MIN_VALUE)));
    }

    @Test
    void positiveLongsFail() {
        assertFalse(ContractRuntime.isNonPositive(1L));
        assertFalse(ContractRuntime.isNonPositive(Long.MAX_VALUE));
    }

    @Test
    void zeroAndNegativeDoublesPass() {
        assertTrue(ContractRuntime.isNonPositive(0.0));
        assertTrue(ContractRuntime.isNonPositive(-1.0));
        assertTrue(ContractRuntime.isNonPositive(-Double.MAX_VALUE));
    }

    @Test
    void negativeZeroDoubleIsNotNonPositive() {
        // -0.0 < 0, so it's negative, not non-positive (which means <= 0)
        // Actually, -0.0 <= 0 is true in Java's comparison
        assertTrue(ContractRuntime.isNonPositive(-0.0));
    }

    @Test
    void zeroDoubleIsNonPositive() {
        assertTrue(ContractRuntime.isNonPositive(0.0));
    }

    @Test
    void nanDoubleIsNotNonPositive() {
        assertFalse(ContractRuntime.isNonPositive(Double.NaN));
    }

    @Test
    void infinityDoublesBehavior() {
        // Positive infinity > 0, so not non-positive
        assertFalse(ContractRuntime.isNonPositive(Double.POSITIVE_INFINITY));
        // Negative infinity < 0, so it IS non-positive
        assertTrue(ContractRuntime.isNonPositive(Double.NEGATIVE_INFINITY));
    }

    @Test
    void positiveDoublesFail() {
        assertFalse(ContractRuntime.isNonPositive(1.0));
        assertFalse(ContractRuntime.isNonPositive(Double.MAX_VALUE));
    }

    @Test
    void zeroAndNegativeFloatsPass() {
        assertTrue(ContractRuntime.isNonPositive(0.0f));
        assertTrue(ContractRuntime.isNonPositive(-1.0f));
        assertTrue(ContractRuntime.isNonPositive(-Float.MAX_VALUE));
    }

    @Test
    void negativeZeroFloatIsNotNonPositive() {
        assertTrue(ContractRuntime.isNonPositive(-0.0f));
    }

    @Test
    void zeroFloatIsNonPositive() {
        assertTrue(ContractRuntime.isNonPositive(0.0f));
    }

    @Test
    void nanFloatIsNotNonPositive() {
        assertFalse(ContractRuntime.isNonPositive(Float.NaN));
    }

    @Test
    void infinityFloatsBehavior() {
        assertFalse(ContractRuntime.isNonPositive(Float.POSITIVE_INFINITY));
        assertTrue(ContractRuntime.isNonPositive(Float.NEGATIVE_INFINITY));
    }

    @Test
    void positiveFloatsFail() {
        assertFalse(ContractRuntime.isNonPositive(1.0f));
    }

    @Test
    void zeroAndNegativeBigDecimalsPass() {
        assertTrue(ContractRuntime.isNonPositive(BigDecimal.ZERO));
        assertTrue(ContractRuntime.isNonPositive(new BigDecimal("-0.0001")));
        assertTrue(ContractRuntime.isNonPositive(BigDecimal.ONE.negate()));
    }

    @Test
    void positiveBigDecimalsFail() {
        assertFalse(ContractRuntime.isNonPositive(BigDecimal.ONE));
        assertFalse(ContractRuntime.isNonPositive(new BigDecimal("0.0001")));
    }

    @Test
    void zeroAndNegativeBigIntegersPass() {
        assertTrue(ContractRuntime.isNonPositive(BigInteger.ZERO));
        assertTrue(ContractRuntime.isNonPositive(BigInteger.ONE.negate()));
        assertTrue(ContractRuntime.isNonPositive(new BigInteger("-999999999999999999999")));
    }

    @Test
    void positiveBigIntegersFail() {
        assertFalse(ContractRuntime.isNonPositive(BigInteger.ONE));
        assertFalse(ContractRuntime.isNonPositive(new BigInteger("999999999999999999999")));
    }

    @Test
    void zeroAndNegativeShortsPass() {
        assertTrue(ContractRuntime.isNonPositive((short) 0));
        assertTrue(ContractRuntime.isNonPositive((short) -1));
        assertTrue(ContractRuntime.isNonPositive(Short.MIN_VALUE));
    }

    @Test
    void positiveShortsFail() {
        assertFalse(ContractRuntime.isNonPositive((short) 1));
        assertFalse(ContractRuntime.isNonPositive(Short.MAX_VALUE));
    }

    @Test
    void zeroAndNegativeBytesPass() {
        assertTrue(ContractRuntime.isNonPositive((byte) 0));
        assertTrue(ContractRuntime.isNonPositive((byte) -1));
        assertTrue(ContractRuntime.isNonPositive(Byte.MIN_VALUE));
    }

    @Test
    void positiveBytesFail() {
        assertFalse(ContractRuntime.isNonPositive((byte) 1));
        assertFalse(ContractRuntime.isNonPositive(Byte.MAX_VALUE));
    }

    @Test
    void unsupportedTypesFail() {
        assertFalse(ContractRuntime.isNonPositive("0"));
        assertFalse(ContractRuntime.isNonPositive(false));
    }
}
