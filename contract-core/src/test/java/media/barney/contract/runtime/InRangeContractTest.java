package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class InRangeContractTest {

    @Test
    void supportsIntegralDecimalAndFloatingPointValues() {
        assertTrue(ContractRuntime.isInRange(BigDecimal.ONE, 0, 2, true, false));
        assertTrue(ContractRuntime.isInRange(BigInteger.ONE, 0, 2, true, false));
        assertTrue(ContractRuntime.isInRange(1.5d, 0, 2, true, false));
        assertTrue(ContractRuntime.isInRange(1.5f, 0, 2, true, false));
        assertTrue(ContractRuntime.isInRange(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE, false, true));
    }

    @Test
    void honorsInclusiveAndExclusiveBoundsForSignedZero() {
        assertTrue(ContractRuntime.isInRange(-0.0d, 0, 2, true, false));
        assertTrue(ContractRuntime.isInRange(0.0d, 0, 2, true, false));
        assertFalse(ContractRuntime.isInRange(-0.0d, 0, 2, false, false));
        assertFalse(ContractRuntime.isInRange(2.0d, 0, 2, true, false));

        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                -0.0d,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("rangeExclusiveUpper", Double.class)));
    }

    @Test
    void rejectsNanAndUnsupportedValues() {
        assertFalse(ContractRuntime.isInRange(Double.NaN, 0, 2, true, false));
        assertFalse(ContractRuntime.isInRange("1", 0, 2, true, false));
    }

    @Test
    void ignoresNullBoxedValues() {
        assertTrue(ContractRuntime.isInRange(null, 0, 2, true, false));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "limit",
                BuiltInContractTestSupport.parameterAnnotations("rangeExclusiveUpper", Double.class)));
    }
}
