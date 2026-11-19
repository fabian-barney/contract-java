package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PatternContractTest {

    private static final GeneratedCheck IGNORE_CASE = GeneratedCheck.pattern("(?i)usr-[0-9]+");

    @Test
    void supportsInlineRegexFlags() {
        assertTrue(IGNORE_CASE.accepts("USR-42"));
        assertTrue(IGNORE_CASE.accepts("usr-42"));

        assertDoesNotThrow(() -> IGNORE_CASE.requireParameter("usr-42", "com.example.UserService.findUser", "tenant"));
    }

    @Test
    void rejectsNonMatchingAndUnsupportedValues() {
        assertFalse(IGNORE_CASE.accepts("TEN-42"));
        assertFalse(IGNORE_CASE.accepts(42));
        assertThrows(
                IllegalArgumentException.class,
                () -> IGNORE_CASE.requireParameter("TEN-42", "com.example.UserService.findUser", "tenant"));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(IGNORE_CASE.accepts(null));
        assertDoesNotThrow(() -> IGNORE_CASE.requireParameter(null, "com.example.UserService.findUser", "tenant"));
    }

    @Test
    void boundsCompiledPatternCacheBeforeCompilingUncachedPattern() {
        for (int index = 0; index <= 256; index++) {
            String value = "value-" + index;
            assertTrue(GeneratedCheck.pattern(value).accepts(value));
        }
    }
}
