package media.barney.contract.runtime;

import media.barney.contract.runtime.internal.ContractArguments;
import media.barney.contract.runtime.internal.ContractChecks;

/**
 * Contract kinds used by generated enforcement code.
 */
public enum RuntimeContract {
    NOT_EMPTY {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.isNotEmpty(value);
        }
    },
    NOT_BLANK {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.isNotBlank(value);
        }
    },
    POSITIVE {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.isPositive(value);
        }
    },
    NEGATIVE {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.isNegative(value);
        }
    },
    NON_NEGATIVE {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.isNonNegative(value);
        }
    },
    NON_POSITIVE {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.isNonPositive(value);
        }
    },
    IN_RANGE {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.isInRange(
                    value, arguments.min(), arguments.max(), arguments.minInclusive(), arguments.maxInclusive());
        }
    },
    SIZE {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.hasSize(value, arguments.sizeMin(), arguments.sizeMax());
        }
    },
    PATTERN {
        @Override
        boolean isValid(Object value, ContractArguments arguments) {
            return ContractChecks.matchesPattern(value, arguments.regexp());
        }
    };

    abstract boolean isValid(Object value, ContractArguments arguments);
}
