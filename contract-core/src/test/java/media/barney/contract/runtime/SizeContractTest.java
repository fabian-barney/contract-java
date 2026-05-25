package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SizeContractTest {

    @Test
    void supportsCharSequenceCollectionMapAndArrayValues() {
        assertTrue(ContractRuntime.hasSize("ab", 1, 2));
        assertTrue(ContractRuntime.hasSize(new StringBuilder("ab"), 1, 2));
        assertTrue(ContractRuntime.hasSize(List.of("a", "b"), 1, 2));
        assertTrue(ContractRuntime.hasSize(Map.of("a", 1), 1, 2));
        assertTrue(ContractRuntime.hasSize(new long[] {1L, 2L}, 1, 2));
    }

    @Test
    void rejectsOutOfRangeAndUnsupportedValues() {
        assertFalse(ContractRuntime.hasSize("", 1, 2));
        assertFalse(ContractRuntime.hasSize("abc", 1, 2));
        assertFalse(ContractRuntime.hasSize(List.of(), 1, 2));
        assertFalse(ContractRuntime.hasSize(Map.of(), 1, 2));
        assertFalse(ContractRuntime.hasSize(new String[0], 1, 2));
        assertFalse(ContractRuntime.hasSize(42, 1, 2));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(ContractRuntime.hasSize(null, 1, 2));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "tenant",
                BuiltInContractTestSupport.parameterAnnotations("sizeString", String.class)));
    }
}
