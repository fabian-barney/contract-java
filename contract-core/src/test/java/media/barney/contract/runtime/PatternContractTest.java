package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PatternContractTest {

    @Test
    void supportsInlineRegexFlags() {
        assertTrue(ContractRuntime.matchesPattern("USR-42", "(?i)usr-[0-9]+"));
        assertTrue(ContractRuntime.matchesPattern("usr-42", "(?i)usr-[0-9]+"));

        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                "usr-42",
                "com.example.UserService.findUser",
                "tenant",
                BuiltInContractTestSupport.parameterAnnotations("patternIgnoreCase", String.class)));
    }

    @Test
    void rejectsNonMatchingAndUnsupportedValues() {
        assertFalse(ContractRuntime.matchesPattern("TEN-42", "(?i)usr-[0-9]+"));
        assertFalse(ContractRuntime.matchesPattern(42, "(?i)usr-[0-9]+"));

        assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        "TEN-42",
                        "com.example.UserService.findUser",
                        "tenant",
                        BuiltInContractTestSupport.parameterAnnotations("patternIgnoreCase", String.class)));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(ContractRuntime.matchesPattern(null, "(?i)usr-[0-9]+"));
        assertDoesNotThrow(() -> ContractRuntime.requireParameter(
                null,
                "com.example.UserService.findUser",
                "tenant",
                BuiltInContractTestSupport.parameterAnnotations("patternIgnoreCase", String.class)));
    }
}
