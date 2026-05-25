package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotEmptyContractTest {

    @Test
    void supportsStringsCollectionsMapsAndArrays() {
        assertTrue(ContractRuntime.isNotEmpty("text"));
        assertTrue(ContractRuntime.isNotEmpty(new StringBuilder("text")));
        assertTrue(ContractRuntime.isNotEmpty(List.of("a")));
        assertTrue(ContractRuntime.isNotEmpty(Map.of("a", "b")));
        assertTrue(ContractRuntime.isNotEmpty(new String[] {"a"}));
    }

    @Test
    void rejectsEmptyValuesAndUnsupportedTypes() {
        assertFalse(ContractRuntime.isNotEmpty(""));
        assertFalse(ContractRuntime.isNotEmpty(List.of()));
        assertFalse(ContractRuntime.isNotEmpty(Map.of()));
        assertFalse(ContractRuntime.isNotEmpty(new String[0]));
        assertFalse(ContractRuntime.isNotEmpty(42));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(ContractRuntime.isNotEmpty(null));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "tenant",
                BuiltInContractTestSupport.parameterAnnotations("notEmptyString", String.class)));
    }
}
