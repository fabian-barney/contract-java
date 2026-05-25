package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotEmptyContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.isNotEmpty(null));
    }

    @Test
    void emptyStringFails() {
        assertFalse(ContractRuntime.isNotEmpty(""));
    }

    @Test
    void nonEmptyStringPasses() {
        assertTrue(ContractRuntime.isNotEmpty("a"));
        assertTrue(ContractRuntime.isNotEmpty("abc"));
        assertTrue(ContractRuntime.isNotEmpty(" "));
    }

    @Test
    void stringBuilderEmptyFails() {
        assertFalse(ContractRuntime.isNotEmpty(new StringBuilder("")));
    }

    @Test
    void stringBuilderNonEmptyPasses() {
        assertTrue(ContractRuntime.isNotEmpty(new StringBuilder("a")));
        assertTrue(ContractRuntime.isNotEmpty(new StringBuilder("abc")));
    }

    @Test
    void stringBufferEmptyFails() {
        assertFalse(ContractRuntime.isNotEmpty(new StringBuffer("")));
    }

    @Test
    void stringBufferNonEmptyPasses() {
        assertTrue(ContractRuntime.isNotEmpty(new StringBuffer("a")));
    }

    @Test
    void emptyListFails() {
        assertFalse(ContractRuntime.isNotEmpty(List.of()));
        assertFalse(ContractRuntime.isNotEmpty(Collections.emptyList()));
    }

    @Test
    void nonEmptyListPasses() {
        assertTrue(ContractRuntime.isNotEmpty(List.of("a")));
        assertTrue(ContractRuntime.isNotEmpty(List.of("a", "b", "c")));
    }

    @Test
    void emptyMapFails() {
        assertFalse(ContractRuntime.isNotEmpty(Map.of()));
        assertFalse(ContractRuntime.isNotEmpty(Collections.emptyMap()));
    }

    @Test
    void nonEmptyMapPasses() {
        assertTrue(ContractRuntime.isNotEmpty(Map.of("a", 1)));
        assertTrue(ContractRuntime.isNotEmpty(Map.of("a", 1, "b", 2)));
    }

    @Test
    void emptyObjectArrayFails() {
        assertFalse(ContractRuntime.isNotEmpty(new String[]{}));
        assertFalse(ContractRuntime.isNotEmpty(new Object[]{}));
    }

    @Test
    void nonEmptyObjectArrayPasses() {
        assertTrue(ContractRuntime.isNotEmpty(new String[]{"a"}));
        assertTrue(ContractRuntime.isNotEmpty(new Object[]{"a", "b"}));
    }

    @Test
    void emptyIntArrayFails() {
        assertFalse(ContractRuntime.isNotEmpty(new int[]{}));
    }

    @Test
    void nonEmptyIntArrayPasses() {
        assertTrue(ContractRuntime.isNotEmpty(new int[]{0}));
        assertTrue(ContractRuntime.isNotEmpty(new int[]{1, 2, 3}));
    }

    @Test
    void emptyLongArrayFails() {
        assertFalse(ContractRuntime.isNotEmpty(new long[]{}));
    }

    @Test
    void nonEmptyLongArrayPasses() {
        assertTrue(ContractRuntime.isNotEmpty(new long[]{0L}));
        assertTrue(ContractRuntime.isNotEmpty(new long[]{1L, 2L, 3L}));
    }

    @Test
    void emptyDoubleArrayFails() {
        assertFalse(ContractRuntime.isNotEmpty(new double[]{}));
    }

    @Test
    void nonEmptyDoubleArrayPasses() {
        assertTrue(ContractRuntime.isNotEmpty(new double[]{0.0}));
        assertTrue(ContractRuntime.isNotEmpty(new double[]{1.0, 2.0}));
    }

    @Test
    void emptyBooleanArrayFails() {
        assertFalse(ContractRuntime.isNotEmpty(new boolean[]{}));
    }

    @Test
    void nonEmptyBooleanArrayPasses() {
        assertTrue(ContractRuntime.isNotEmpty(new boolean[]{false}));
        assertTrue(ContractRuntime.isNotEmpty(new boolean[]{true, false}));
    }

    @Test
    void arrayWithNullElementsIsNotEmpty() {
        assertTrue(ContractRuntime.isNotEmpty(new String[]{null}));
        assertTrue(ContractRuntime.isNotEmpty(new Object[]{null, null}));
    }

    @Test
    void unsupportedTypeFails() {
        assertFalse(ContractRuntime.isNotEmpty(42));
        assertFalse(ContractRuntime.isNotEmpty(true));
    }

    @Test
    void singleWhitespaceCharacterIsNotEmpty() {
        assertTrue(ContractRuntime.isNotEmpty(" "));
        assertTrue(ContractRuntime.isNotEmpty("\t"));
        assertTrue(ContractRuntime.isNotEmpty("\n"));
    }
}
