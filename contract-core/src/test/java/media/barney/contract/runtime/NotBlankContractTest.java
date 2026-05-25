package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotBlankContractTest {

    @Test
    void requiresVisibleCharacters() {
        assertTrue(ContractRuntime.isNotBlank("tenant"));
        assertTrue(ContractRuntime.isNotBlank(new StringBuilder("tenant")));
        assertFalse(ContractRuntime.isNotBlank(""));
        assertFalse(ContractRuntime.isNotBlank(" \t\n"));
    }

    @Test
    void rejectsUnsupportedTypes() {
        assertFalse(ContractRuntime.isNotBlank(List.of("tenant")));
        assertFalse(ContractRuntime.isNotBlank(42));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(ContractRuntime.isNotBlank(null));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "tenant",
                BuiltInContractTestSupport.parameterAnnotations("notBlankString", String.class)));
    }
}
