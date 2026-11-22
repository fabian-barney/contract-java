package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotBlankContractTest {

    private static final GeneratedCheck NOT_BLANK = GeneratedCheck.notBlank();

    @Test
    void requiresVisibleCharacters() {
        assertTrue(NOT_BLANK.accepts("tenant"));
        assertTrue(NOT_BLANK.accepts(new StringBuilder("tenant")));
        assertFalse(NOT_BLANK.accepts(""));
        assertFalse(NOT_BLANK.accepts(" \t\n"));
    }

    @Test
    void rejectsUnsupportedTypes() {
        assertFalse(NOT_BLANK.accepts(List.of("tenant")));
        assertFalse(NOT_BLANK.accepts(42));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(NOT_BLANK.accepts(null));
        assertDoesNotThrow(() -> NOT_BLANK.requireParameter(null, "com.example.UserService.findUser", "tenant"));
    }
}
