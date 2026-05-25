package media.barney.contract.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;

final class NumericComparison {

    private static final int UNSUPPORTED = Integer.MIN_VALUE;

    private NumericComparison() {}

    static int compareToZero(Object value) {
        if (value instanceof BigDecimal number) {
            return number.compareTo(BigDecimal.ZERO);
        }
        if (value instanceof BigInteger number) {
            return number.compareTo(BigInteger.ZERO);
        }
        if (value instanceof Double number) {
            return compareDouble(number);
        }
        if (value instanceof Float number) {
            return compareDouble(number.doubleValue());
        }
        if (value instanceof Number number) {
            return Long.compare(number.longValue(), 0L);
        }

        return UNSUPPORTED;
    }

    static boolean isSupported(int comparison) {
        return comparison != UNSUPPORTED;
    }

    static boolean isInRange(Object value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        if (value instanceof BigDecimal number) {
            return inBigDecimalRange(number, min, max, minInclusive, maxInclusive);
        }
        if (value instanceof BigInteger number) {
            return inBigIntegerRange(number, min, max, minInclusive, maxInclusive);
        }
        if (value instanceof Double number) {
            return inDoubleRange(number, min, max, minInclusive, maxInclusive);
        }
        if (value instanceof Float number) {
            return inDoubleRange(number.doubleValue(), min, max, minInclusive, maxInclusive);
        }
        if (value instanceof Number number) {
            return inLongRange(number.longValue(), min, max, minInclusive, maxInclusive);
        }

        return false;
    }

    private static int compareDouble(double value) {
        if (Double.isNaN(value)) {
            return UNSUPPORTED;
        }

        return Double.compare(value, 0.0d);
    }

    private static boolean inBigDecimalRange(
            BigDecimal value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return lowerBound(value.compareTo(BigDecimal.valueOf(min)), minInclusive)
                && upperBound(value.compareTo(BigDecimal.valueOf(max)), maxInclusive);
    }

    private static boolean inBigIntegerRange(
            BigInteger value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return lowerBound(value.compareTo(BigInteger.valueOf(min)), minInclusive)
                && upperBound(value.compareTo(BigInteger.valueOf(max)), maxInclusive);
    }

    private static boolean inDoubleRange(double value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        if (Double.isNaN(value)) {
            return false;
        }

        return lowerBound(Double.compare(value, min), minInclusive)
                && upperBound(Double.compare(value, max), maxInclusive);
    }

    private static boolean inLongRange(long value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return lowerBound(Long.compare(value, min), minInclusive) && upperBound(Long.compare(value, max), maxInclusive);
    }

    private static boolean lowerBound(int comparison, boolean inclusive) {
        return inclusive ? comparison >= 0 : comparison > 0;
    }

    private static boolean upperBound(int comparison, boolean inclusive) {
        return inclusive ? comparison <= 0 : comparison < 0;
    }
}
