package media.barney.contract.runtime;

import media.barney.contract.MaskRenderer;
import media.barney.contract.runtime.internal.ContractArguments;
import media.barney.contract.runtime.internal.ContractMessages;
import media.barney.contract.runtime.internal.ContractRule;
import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Runtime support used by generated contract checks.
 *
 * <p>This class is part of the supported generated-code API. Application code
 * normally declares contracts with {@code media.barney.contract.Contract};
 * annotation-processor-generated checks call this bridge to evaluate
 * contracts and throw the documented exception types.
 *
 * <p>Object-valued bridge methods are null-safe. A {@code null} value is
 * treated as passing because nullness is outside the contract model.
 */
@API(status = API.Status.MAINTAINED)
public final class ContractRuntime {

    private ContractRuntime() {
        throw new AssertionError("No instances.");
    }

    /**
     * Evaluates a generated parameter contract with pre-resolved metadata.
     *
     * <p>Generated code calls this entry point with the runtime contract kind,
     * message description, mask renderer, and attribute values that the
     * annotation processor resolved from source annotations.
     *
     * @param value the parameter value to evaluate; {@code null} passes
     *     semantic contracts
     * @param methodName the fully qualified executable name used in messages
     * @param parameterName the parameter name used in messages
     * @param contract the contract kind to evaluate
     * @param description the generated or custom violation description
     * @param customDescription whether {@code description} came from a custom
     *     annotation message
     * @param maskRenderer renderer type for confidential values, or
     *     {@code null} when no masking applies
     * @param min numeric lower bound for range contracts
     * @param max numeric upper bound for range contracts
     * @param minInclusive whether the numeric lower bound is inclusive
     * @param maxInclusive whether the numeric upper bound is inclusive
     * @param sizeMin inclusive lower bound for size contracts
     * @param sizeMax inclusive upper bound for size contracts
     * @param regexp regular expression for pattern contracts
     * @throws IllegalArgumentException when the parameter contract fails
     */
    public static void requireParameterValue(
            @Nullable Object value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    /**
     * Evaluates a generated parameter contract without boxing a primitive value
     * on the successful validation path.
     */
    public static void requireParameterValue(
            byte value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    /**
     * Evaluates a generated parameter contract without boxing a primitive value
     * on the successful validation path.
     */
    public static void requireParameterValue(
            short value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    /**
     * Evaluates a generated parameter contract without boxing a primitive value
     * on the successful validation path.
     */
    public static void requireParameterValue(
            int value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    /**
     * Evaluates a generated parameter contract without boxing a primitive value
     * on the successful validation path.
     */
    public static void requireParameterValue(
            long value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    /**
     * Evaluates a generated parameter contract without boxing a primitive value
     * on the successful validation path.
     */
    public static void requireParameterValue(
            float value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    /**
     * Evaluates a generated parameter contract without boxing a primitive value
     * on the successful validation path.
     */
    public static void requireParameterValue(
            double value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    /**
     * Evaluates a generated return-value contract with pre-resolved metadata.
     *
     * <p>Generated code calls this entry point with the runtime contract kind,
     * message description, mask renderer, and attribute values that the
     * annotation processor resolved from source annotations. The original value
     * is returned unchanged when the contract passes so generated return
     * statements can remain expression-shaped.
     *
     * @param <T> the return-value type
     * @param value the return value to evaluate; {@code null} passes semantic
     *     contracts
     * @param methodName the fully qualified method name used in messages
     * @param contract the contract kind to evaluate
     * @param description the generated or custom violation description
     * @param customDescription whether {@code description} came from a custom
     *     annotation message
     * @param maskRenderer renderer type for confidential values, or
     *     {@code null} when no masking applies
     * @param min numeric lower bound for range contracts
     * @param max numeric upper bound for range contracts
     * @param minInclusive whether the numeric lower bound is inclusive
     * @param maxInclusive whether the numeric upper bound is inclusive
     * @param sizeMin inclusive lower bound for size contracts
     * @param sizeMax inclusive upper bound for size contracts
     * @param regexp regular expression for pattern contracts
     * @return {@code value} when the contract passes
     * @throws IllegalStateException when the return-value contract fails
     */
    public static <T extends @Nullable Object> T requireReturnValue(
            T value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }

    /**
     * Evaluates a generated primitive return-value contract without boxing the
     * value on the successful validation path.
     */
    public static byte requireReturnValue(
            byte value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }

    /**
     * Evaluates a generated primitive return-value contract without boxing the
     * value on the successful validation path.
     */
    public static short requireReturnValue(
            short value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }

    /**
     * Evaluates a generated primitive return-value contract without boxing the
     * value on the successful validation path.
     */
    public static int requireReturnValue(
            int value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }

    /**
     * Evaluates a generated primitive return-value contract without boxing the
     * value on the successful validation path.
     */
    public static long requireReturnValue(
            long value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }

    /**
     * Evaluates a generated primitive return-value contract without boxing the
     * value on the successful validation path.
     */
    public static float requireReturnValue(
            float value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }

    /**
     * Evaluates a generated primitive return-value contract without boxing the
     * value on the successful validation path.
     */
    public static double requireReturnValue(
            double value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            @Nullable Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            @Nullable String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }
}
