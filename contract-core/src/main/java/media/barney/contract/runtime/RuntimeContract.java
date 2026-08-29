package media.barney.contract.runtime;

import media.barney.contract.runtime.internal.ContractArguments;
import media.barney.contract.runtime.internal.ContractChecks;
import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Contract kinds used by generated enforcement code.
 *
 * <p>Each value maps a built-in contract annotation to the corresponding
 * runtime evaluation rule. This enum is part of the generated-code bridge;
 * application code normally uses {@code media.barney.contract.Contract}
 * annotations directly.
 */
@API(status = API.Status.MAINTAINED)
public enum RuntimeContract {
    /**
     * Non-empty contract for character sequences, collections, maps, and arrays.
     */
    NOT_EMPTY {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.isNotEmpty(value);
        }
    },
    /**
     * Non-blank contract for character sequences.
     */
    NOT_BLANK {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.isNotBlank(value);
        }
    },
    /**
     * Numeric contract requiring a value greater than zero.
     */
    POSITIVE {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.isPositive(value);
        }
    },
    /**
     * Numeric contract requiring a value less than zero.
     */
    NEGATIVE {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.isNegative(value);
        }
    },
    /**
     * Numeric contract requiring a value greater than or equal to zero.
     */
    NON_NEGATIVE {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.isNonNegative(value);
        }
    },
    /**
     * Numeric contract requiring a value less than or equal to zero.
     */
    NON_POSITIVE {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.isNonPositive(value);
        }
    },
    /**
     * Numeric contract requiring a value within configured bounds.
     */
    IN_RANGE {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.isInRange(
                    value, arguments.min(), arguments.max(), arguments.minInclusive(), arguments.maxInclusive());
        }
    },
    /**
     * Size contract for character sequences, collections, maps, and arrays.
     */
    SIZE {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.hasSize(value, arguments.sizeMin(), arguments.sizeMax());
        }
    },
    /**
     * Pattern contract requiring a character sequence to fully match a regular expression.
     */
    PATTERN {
        @Override
        boolean isValid(@Nullable Object value, ContractArguments arguments) {
            return ContractChecks.matchesPattern(value, arguments.regexp());
        }
    };

    abstract boolean isValid(@Nullable Object value, ContractArguments arguments);

    boolean isValid(long value, ContractArguments arguments) {
        if (this == POSITIVE) {
            return value > 0L;
        }
        if (this == NEGATIVE) {
            return value < 0L;
        }
        if (this == NON_NEGATIVE) {
            return value >= 0L;
        }
        if (this == NON_POSITIVE) {
            return value <= 0L;
        }
        if (this == IN_RANGE) {
            return ContractChecks.isInRange(
                    value, arguments.min(), arguments.max(), arguments.minInclusive(), arguments.maxInclusive());
        }

        return false;
    }

    boolean isValid(double value, ContractArguments arguments) {
        if (this == POSITIVE) {
            return value > 0.0d;
        }
        if (this == NEGATIVE) {
            return value < 0.0d;
        }
        if (this == NON_NEGATIVE) {
            return value >= 0.0d;
        }
        if (this == NON_POSITIVE) {
            return value <= 0.0d;
        }
        if (this == IN_RANGE) {
            return ContractChecks.isInRange(
                    value, arguments.min(), arguments.max(), arguments.minInclusive(), arguments.maxInclusive());
        }

        return false;
    }
}
