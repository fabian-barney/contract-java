package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SizeContractTest {

    private static final GeneratedCheck SIZE = GeneratedCheck.size(1, 2);

    @Test
    void supportsCharSequenceCollectionMapAndArrayValues() {
        assertTrue(SIZE.accepts("ab"));
        assertTrue(SIZE.accepts(new StringBuilder("ab")));
        assertTrue(SIZE.accepts(List.of("a", "b")));
        assertTrue(SIZE.accepts(Map.of("a", 1)));
        assertTrue(SIZE.accepts(new long[] {1L, 2L}));
    }

    @Test
    void rejectsOutOfRangeAndUnsupportedValues() {
        assertFalse(SIZE.accepts(""));
        assertFalse(SIZE.accepts("abc"));
        assertFalse(SIZE.accepts(List.of()));
        assertFalse(SIZE.accepts(Map.of()));
        assertFalse(SIZE.accepts(new String[0]));
        assertFalse(SIZE.accepts(42));
    }

    @Test
    void ignoresNullReferenceValues() {
        assertTrue(SIZE.accepts(null));
        assertDoesNotThrow(() -> SIZE.requireParameter(null, "com.example.UserService.findUser", "tenant"));
    }
}
