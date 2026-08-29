package media.barney.contract.runtime.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public final class ContractChecks {

    private static final int MAX_PATTERN_CACHE_SIZE = 256;
    private static final ConcurrentMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private ContractChecks() {}

    public static boolean isNotEmpty(Object value) {
        if (value == null) {
            return true;
        }

        return ValueSize.sizeOf(value) > 0;
    }

    public static boolean isNotBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (!(value instanceof CharSequence sequence)) {
            return false;
        }

        return containsNonWhitespace(sequence);
    }

    public static boolean isPositive(Object value) {
        if (value == null) {
            return true;
        }

        int comparison = NumericComparison.compareToZero(value);
        return NumericComparison.isSupported(comparison) && comparison > 0;
    }

    public static boolean isNegative(Object value) {
        if (value == null) {
            return true;
        }

        int comparison = NumericComparison.compareToZero(value);
        return NumericComparison.isSupported(comparison) && comparison < 0;
    }

    public static boolean isNonNegative(Object value) {
        if (value == null) {
            return true;
        }

        int comparison = NumericComparison.compareToZero(value);
        return NumericComparison.isSupported(comparison) && comparison >= 0;
    }

    public static boolean isNonPositive(Object value) {
        if (value == null) {
            return true;
        }

        int comparison = NumericComparison.compareToZero(value);
        return NumericComparison.isSupported(comparison) && comparison <= 0;
    }

    public static boolean isInRange(Object value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        if (value == null) {
            return true;
        }

        return NumericComparison.isInRange(value, min, max, minInclusive, maxInclusive);
    }

    public static boolean isInRange(long value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return NumericComparison.isInRange(value, min, max, minInclusive, maxInclusive);
    }

    public static boolean isInRange(double value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return NumericComparison.isInRange(value, min, max, minInclusive, maxInclusive);
    }

    public static boolean hasSize(Object value, int min, int max) {
        if (value == null) {
            return true;
        }

        int size = ValueSize.sizeOf(value);
        return size >= 0 && size >= min && size <= max;
    }

    public static boolean matchesPattern(Object value, String regexp) {
        if (value == null) {
            return true;
        }
        if (!(value instanceof CharSequence sequence)) {
            return false;
        }

        return compiledPattern(regexp).matcher(sequence).matches();
    }

    private static Pattern compiledPattern(String regexp) {
        if (PATTERN_CACHE.size() >= MAX_PATTERN_CACHE_SIZE && !PATTERN_CACHE.containsKey(regexp)) {
            return Pattern.compile(regexp);
        }

        return PATTERN_CACHE.computeIfAbsent(regexp, Pattern::compile);
    }

    private static boolean containsNonWhitespace(CharSequence sequence) {
        for (int index = 0; index < sequence.length(); index++) {
            if (!Character.isWhitespace(sequence.charAt(index))) {
                return true;
            }
        }

        return false;
    }
}
