package media.barney.contract.runtime;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

final class ValueSize {

    private ValueSize() {
    }

    static int sizeOf(Object value) {
        if (value instanceof CharSequence sequence) {
            return sequence.length();
        }
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        if (value != null && value.getClass().isArray()) {
            return Array.getLength(value);
        }

        return -1;
    }
}
