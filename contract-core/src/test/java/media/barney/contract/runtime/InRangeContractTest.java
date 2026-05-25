package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class InRangeContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.isInRange(null, 1, 10, true, true));
    }

    @Test
    void integerValueWithinInclusiveRangePasses() {
        assertTrue(ContractRuntime.isInRange(5, 1, 10, true, true));
        assertTrue(ContractRuntime.isInRange(1, 1, 10, true, true));
        assertTrue(ContractRuntime.isInRange(10, 1, 10, true, true));
    }

    @Test
    void integerValueWithinExclusiveRangePasses() {
        assertTrue(ContractRuntime.isInRange(5, 1, 10, false, false));
    }

    @Test
    void integerBoundaryValuesWithExclusiveRangeFail() {
        assertFalse(ContractRuntime.isInRange(1, 1, 10, false, true));
        assertFalse(ContractRuntime.isInRange(10, 1, 10, true, false));
    }

    @Test
    void integerValueOutsideRangeFails() {
        assertFalse(ContractRuntime.isInRange(0, 1, 10, true, true));
        assertFalse(ContractRuntime.isInRange(11, 1, 10, true, true));
        assertFalse(ContractRuntime.isInRange(Integer.MIN_VALUE, 1, 10, true, true));
        assertFalse(ContractRuntime.isInRange(Integer.MAX_VALUE, 1, 10, true, true));
    }

    @Test
    void boxedIntegerWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange(Integer.valueOf(5), 1, 10, true, true));
    }

    @Test
    void longValueWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange(5L, 1L, 10L, true, true));
        assertTrue(ContractRuntime.isInRange(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, true, true));
    }

    @Test
    void longValueOutsideRangeFails() {
        assertFalse(ContractRuntime.isInRange(0L, 1L, 10L, true, true));
        assertFalse(ContractRuntime.isInRange(11L, 1L, 10L, true, true));
    }

    @Test
    void doubleValueWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange(5.5, 1L, 10L, true, true));
        assertTrue(ContractRuntime.isInRange(1.0, 1L, 10L, true, true));
        assertTrue(ContractRuntime.isInRange(10.0, 1L, 10L, true, true));
    }

    @Test
    void nanDoubleFails() {
        assertFalse(ContractRuntime.isInRange(Double.NaN, 1L, 10L, true, true));
    }

    @Test
    void infinityDoubleFails() {
        assertFalse(ContractRuntime.isInRange(Double.POSITIVE_INFINITY, 1L, 10L, true, true));
        assertFalse(ContractRuntime.isInRange(Double.NEGATIVE_INFINITY, 1L, 10L, true, true));
    }

    @Test
    void doubleValueOutsideRangeFails() {
        assertFalse(ContractRuntime.isInRange(0.5, 1L, 10L, true, true));
        assertFalse(ContractRuntime.isInRange(10.5, 1L, 10L, true, true));
    }

    @Test
    void floatValueWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange(5.5f, 1L, 10L, true, true));
    }

    @Test
    void nanFloatFails() {
        assertFalse(ContractRuntime.isInRange(Float.NaN, 1L, 10L, true, true));
    }

    @Test
    void bigDecimalWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange(new BigDecimal("5.5"), 1, 10, true, true));
        assertTrue(ContractRuntime.isInRange(BigDecimal.ONE, 1, 10, true, true));
        assertTrue(ContractRuntime.isInRange(new BigDecimal("10"), 1, 10, true, true));
    }

    @Test
    void bigDecimalOutsideRangeFails() {
        assertFalse(ContractRuntime.isInRange(BigDecimal.ZERO, 1, 10, true, true));
        assertFalse(ContractRuntime.isInRange(new BigDecimal("10.5"), 1, 10, true, true));
    }

    @Test
    void bigIntegerWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange(BigInteger.valueOf(5), 1, 10, true, true));
        assertTrue(ContractRuntime.isInRange(BigInteger.ONE, 1, 10, true, true));
    }

    @Test
    void bigIntegerOutsideRangeFails() {
        assertFalse(ContractRuntime.isInRange(BigInteger.ZERO, 1, 10, true, true));
        assertFalse(ContractRuntime.isInRange(BigInteger.TEN, 1, 9, true, true));
    }

    @Test
    void shortValueWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange((short) 5, 1, 10, true, true));
    }

    @Test
    void shortValueOutsideRangeFails() {
        assertFalse(ContractRuntime.isInRange((short) 0, 1, 10, true, true));
    }

    @Test
    void byteValueWithinRangePasses() {
        assertTrue(ContractRuntime.isInRange((byte) 5, 1, 10, true, true));
    }

    @Test
    void byteValueOutsideRangeFails() {
        assertFalse(ContractRuntime.isInRange((byte) 0, 1, 10, true, true));
    }

    @Test
    void mixedInclusivityRangesWork() {
        assertTrue(ContractRuntime.isInRange(5, 1, 10, false, false));
        assertFalse(ContractRuntime.isInRange(1, 1, 10, false, false));
        assertFalse(ContractRuntime.isInRange(10, 1, 10, false, false));
        
        assertTrue(ContractRuntime.isInRange(1, 1, 10, true, false));
        assertFalse(ContractRuntime.isInRange(10, 1, 10, true, false));
        
        assertTrue(ContractRuntime.isInRange(10, 1, 10, false, true));
        assertFalse(ContractRuntime.isInRange(1, 1, 10, false, true));
    }

    @Test
    void reversedRangeAlwaysFails() {
        assertFalse(ContractRuntime.isInRange(5, 10, 1, true, true));
    }

    @Test
    void unsupportedTypesFail() {
        assertFalse(ContractRuntime.isInRange("5", 1, 10, true, true));
        assertFalse(ContractRuntime.isInRange(true, 1, 10, true, true));
    }
}
