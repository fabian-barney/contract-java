package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotEmptyContractTest {

    private static final GeneratedCheck NOT_EMPTY = GeneratedCheck.notEmpty();

    @Test
    void supportsStringsCollectionsMapsAndArrays() {
        assertTrue(NOT_EMPTY.accepts("text"));
        assertTrue(NOT_EMPTY.accepts(new StringBuilder("text")));
        assertTrue(NOT_EMPTY.accepts(List.of("a")));
        assertTrue(NOT_EMPTY.accepts(Map.of("a", "b")));
        assertTrue(NOT_EMPTY.accepts(new String[] {"a"}));
    }

    @Test
    void rejectsEmptyValuesAndUnsupportedTypes() {
        assertFalse(NOT_EMPTY.accepts(""));
        assertFalse(NOT_EMPTY.accepts(List.of()));
        assertFalse(NOT_EMPTY.accepts(Map.of()));
        assertFalse(NOT_EMPTY.accepts(new String[0]));
        assertFalse(NOT_EMPTY.accepts(42));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(NOT_EMPTY.accepts(null));
        assertDoesNotThrow(() -> NOT_EMPTY.requireParameter(null, "com.example.UserService.findUser", "tenant"));
    }
}
