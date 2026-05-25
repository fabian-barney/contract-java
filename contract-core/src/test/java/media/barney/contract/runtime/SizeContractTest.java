package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SizeContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.hasSize(null, 1, 10));
    }

    @Test
    void emptyStringWithMinZeroPasses() {
        assertTrue(ContractRuntime.hasSize("", 0, 10));
        assertTrue(ContractRuntime.hasSize("", 0, 0));
    }

    @Test
    void emptyStringWithMinOneFails() {
        assertFalse(ContractRuntime.hasSize("", 1, 10));
    }

    @Test
    void nonEmptyStringWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize("a", 1, 10));
        assertTrue(ContractRuntime.hasSize("abc", 1, 10));
        assertTrue(ContractRuntime.hasSize("abcdefghij", 1, 10));
    }

    @Test
    void stringTooShortFails() {
        assertFalse(ContractRuntime.hasSize("a", 2, 10));
        assertFalse(ContractRuntime.hasSize("", 1, 10));
    }

    @Test
    void stringTooLongFails() {
        assertFalse(ContractRuntime.hasSize("abcdefghijk", 1, 10));
        assertFalse(ContractRuntime.hasSize("abcdefghijklmnopqrstuvwxyz", 1, 10));
    }

    @Test
    void stringBuilderWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(new StringBuilder("abc"), 1, 10));
        assertTrue(ContractRuntime.hasSize(new StringBuilder(""), 0, 10));
    }

    @Test
    void stringBufferWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(new StringBuffer("abc"), 1, 10));
    }

    @Test
    void emptyListWithMinZeroPasses() {
        assertTrue(ContractRuntime.hasSize(List.of(), 0, 10));
        assertTrue(ContractRuntime.hasSize(Collections.emptyList(), 0, 10));
    }

    @Test
    void emptyListWithMinOneFails() {
        assertFalse(ContractRuntime.hasSize(List.of(), 1, 10));
        assertFalse(ContractRuntime.hasSize(Collections.emptyList(), 1, 10));
    }

    @Test
    void listWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(List.of("a"), 1, 10));
        assertTrue(ContractRuntime.hasSize(List.of("a", "b", "c"), 1, 10));
    }

    @Test
    void listTooLargeFails() {
        assertFalse(ContractRuntime.hasSize(List.of("a", "b", "c", "d", "e"), 1, 3));
    }

    @Test
    void emptyMapWithMinZeroPasses() {
        assertTrue(ContractRuntime.hasSize(Map.of(), 0, 10));
        assertTrue(ContractRuntime.hasSize(Collections.emptyMap(), 0, 10));
    }

    @Test
    void emptyMapWithMinOneFails() {
        assertFalse(ContractRuntime.hasSize(Map.of(), 1, 10));
    }

    @Test
    void mapWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(Map.of("a", 1), 1, 10));
        assertTrue(ContractRuntime.hasSize(Map.of("a", 1, "b", 2), 1, 10));
    }

    @Test
    void mapTooLargeFails() {
        assertFalse(ContractRuntime.hasSize(Map.of("a", 1, "b", 2, "c", 3), 1, 2));
    }

    @Test
    void emptyArrayWithMinZeroPasses() {
        assertTrue(ContractRuntime.hasSize(new String[]{}, 0, 10));
        assertTrue(ContractRuntime.hasSize(new int[]{}, 0, 10));
    }

    @Test
    void emptyArrayWithMinOneFails() {
        assertFalse(ContractRuntime.hasSize(new String[]{}, 1, 10));
        assertFalse(ContractRuntime.hasSize(new int[]{}, 1, 10));
    }

    @Test
    void objectArrayWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(new String[]{"a"}, 1, 10));
        assertTrue(ContractRuntime.hasSize(new String[]{"a", "b", "c"}, 1, 10));
    }

    @Test
    void intArrayWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(new int[]{1}, 1, 10));
        assertTrue(ContractRuntime.hasSize(new int[]{1, 2, 3}, 1, 10));
    }

    @Test
    void longArrayWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(new long[]{1L}, 1, 10));
        assertTrue(ContractRuntime.hasSize(new long[]{1L, 2L, 3L}, 1, 10));
    }

    @Test
    void doubleArrayWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(new double[]{1.0}, 1, 10));
    }

    @Test
    void booleanArrayWithinRangePasses() {
        assertTrue(ContractRuntime.hasSize(new boolean[]{true}, 1, 10));
    }

    @Test
    void arrayTooLargeFails() {
        assertFalse(ContractRuntime.hasSize(new int[]{1, 2, 3, 4, 5}, 1, 3));
    }

    @Test
    void boundaryValuesWork() {
        assertTrue(ContractRuntime.hasSize("abc", 3, 3));
        assertFalse(ContractRuntime.hasSize("ab", 3, 3));
        assertFalse(ContractRuntime.hasSize("abcd", 3, 3));
    }

    @Test
    void largeMaxValueWorks() {
        assertTrue(ContractRuntime.hasSize("a", 0, Integer.MAX_VALUE));
        assertTrue(ContractRuntime.hasSize(new int[1000], 0, Integer.MAX_VALUE));
    }

    @Test
    void unsupportedTypeFails() {
        assertFalse(ContractRuntime.hasSize(42, 0, 10));
        assertFalse(ContractRuntime.hasSize(true, 0, 10));
    }

    @Test
    void reversedRangeAlwaysFails() {
        assertFalse(ContractRuntime.hasSize("abc", 10, 1));
    }
}
