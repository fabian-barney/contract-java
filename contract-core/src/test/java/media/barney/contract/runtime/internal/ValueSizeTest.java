package media.barney.contract.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ValueSizeTest {

    @Test
    void reportsUnsupportedAndNullValuesAsUnknown() {
        assertEquals(-1, ValueSize.sizeOf(null));
        assertEquals(-1, ValueSize.sizeOf(42));
    }
}
