package media.barney.contract.runtime;

import media.barney.contract.MaskRenderer;
import media.barney.contract.runtime.internal.ContractArguments;
import org.jspecify.annotations.Nullable;

/**
 * Issues the object-valued bridge calls that annotation-processor-generated
 * checks emit for reference types.
 *
 * <p>The processor resolves the contract kind, description, mask renderer, and
 * attribute values at compile time; this fixture carries those pre-resolved
 * values to the {@code Object} parameter bridge and the generic return bridge.
 * Primitive arguments are boxed here, so the primitive bridge overloads are
 * covered by {@code ContractRuntimeTest} and {@code ContractProcessorIT}
 * instead.
 */
record GeneratedCheck(
        RuntimeContract kind,
        String description,
        boolean customDescription,
        @Nullable Class<? extends MaskRenderer> maskRenderer,
        ContractArguments arguments) {

    private static final ContractArguments NO_ARGUMENTS = new ContractArguments(0L, 0L, true, true, 0, 0, null);

    static GeneratedCheck notEmpty() {
        return simple(RuntimeContract.NOT_EMPTY, "must not be empty");
    }

    static GeneratedCheck notBlank() {
        return simple(RuntimeContract.NOT_BLANK, "must not be blank");
    }

    static GeneratedCheck positive() {
        return simple(RuntimeContract.POSITIVE, "must be positive");
    }

    static GeneratedCheck negative() {
        return simple(RuntimeContract.NEGATIVE, "must be negative");
    }

    static GeneratedCheck nonNegative() {
        return simple(RuntimeContract.NON_NEGATIVE, "must be non-negative");
    }

    static GeneratedCheck nonPositive() {
        return simple(RuntimeContract.NON_POSITIVE, "must be non-positive");
    }

    static GeneratedCheck inRange(long min, long max, boolean minInclusive, boolean maxInclusive) {
        String description =
                "must be within " + (minInclusive ? "[" : "(") + min + ", " + max + (maxInclusive ? "]" : ")");
        return new GeneratedCheck(
                RuntimeContract.IN_RANGE,
                description,
                false,
                null,
                new ContractArguments(min, max, minInclusive, maxInclusive, 0, 0, null));
    }

    static GeneratedCheck size(int min, int max) {
        return new GeneratedCheck(
                RuntimeContract.SIZE,
                "must have size within [" + min + ", " + max + "]",
                false,
                null,
                new ContractArguments(0L, 0L, true, true, min, max, null));
    }

    static GeneratedCheck pattern(String regexp) {
        return new GeneratedCheck(
                RuntimeContract.PATTERN,
                "must match the required pattern",
                false,
                null,
                new ContractArguments(0L, 0L, true, true, 0, 0, regexp));
    }

    private static GeneratedCheck simple(RuntimeContract kind, String description) {
        return new GeneratedCheck(kind, description, false, null, NO_ARGUMENTS);
    }

    GeneratedCheck withMessage(String message) {
        return new GeneratedCheck(kind, message, true, maskRenderer, arguments);
    }

    GeneratedCheck masked(Class<? extends MaskRenderer> renderer) {
        return new GeneratedCheck(kind, description, customDescription, renderer, arguments);
    }

    boolean accepts(@Nullable Object value) {
        return kind.isValid(value, arguments);
    }

    void requireParameter(@Nullable Object value, String methodName, String parameterName) {
        ContractRuntime.requireParameterValue(
                value,
                methodName,
                parameterName,
                kind,
                description,
                customDescription,
                maskRenderer,
                arguments.min(),
                arguments.max(),
                arguments.minInclusive(),
                arguments.maxInclusive(),
                arguments.sizeMin(),
                arguments.sizeMax(),
                arguments.regexp());
    }

    @Nullable Object requireReturn(@Nullable Object value, String methodName) {
        return ContractRuntime.requireReturnValue(
                value,
                methodName,
                kind,
                description,
                customDescription,
                maskRenderer,
                arguments.min(),
                arguments.max(),
                arguments.minInclusive(),
                arguments.maxInclusive(),
                arguments.sizeMin(),
                arguments.sizeMax(),
                arguments.regexp());
    }
}
