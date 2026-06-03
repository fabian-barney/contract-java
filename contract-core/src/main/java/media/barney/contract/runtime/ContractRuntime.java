package media.barney.contract.runtime;

import java.lang.annotation.Annotation;
import media.barney.contract.MaskRenderer;
import media.barney.contract.runtime.internal.ContractAnnotations;
import media.barney.contract.runtime.internal.ContractArguments;
import media.barney.contract.runtime.internal.ContractChecks;
import media.barney.contract.runtime.internal.ContractEvaluation;
import media.barney.contract.runtime.internal.ContractMessages;
import media.barney.contract.runtime.internal.ContractRule;
import media.barney.contract.runtime.internal.ValueRenderer;
import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Runtime support used by generated contract checks.
 *
 * <p>This class is part of the supported generated-code API. Application code
 * normally declares contracts with {@code media.barney.contract.Contract};
 * annotation-processor-generated checks call this bridge to evaluate
 * contracts, render values, and create documented exception types.
 *
 * <p>All semantic check methods are null-safe for object values. A
 * {@code null} value is treated as passing because nullness is outside the
 * contract model.
 */
@API(status = API.Status.MAINTAINED)
public final class ContractRuntime {

    private ContractRuntime() {
        throw new AssertionError("No instances.");
    }

    /**
     * Evaluates annotations applied to a method or constructor parameter.
     *
     * <p>If a semantic contract fails, this method throws an
     * {@link IllegalArgumentException} whose message includes the method name,
     * parameter name, contract description, and rendered value. If a
     * {@code Mask} annotation applies, the value is rendered through the
     * configured mask renderer.
     *
     * @param value the parameter value to evaluate; {@code null} passes
     *     semantic contracts
     * @param methodName the fully qualified executable name used in messages
     * @param parameterName the parameter name used in messages
     * @param annotations the annotations applied to the parameter
     * @throws IllegalArgumentException when a parameter contract fails
     */
    public static void requireParameter(
            @Nullable Object value, String methodName, String parameterName, Annotation... annotations) {
        ContractEvaluation evaluation = ContractAnnotations.evaluate(value, annotations);

        if (!evaluation.valid()) {
            throw ContractMessages.preconditionViolation(
                    methodName, parameterName, evaluation.rule(), value, evaluation.maskRenderer());
        }
    }

    /**
     * Evaluates annotations applied to a non-void method return value.
     *
     * <p>If a semantic contract fails, this method throws an
     * {@link IllegalStateException} whose message includes the method name,
     * contract description, and rendered return value. If a {@code Mask}
     * annotation applies, the value is rendered through the configured mask
     * renderer.
     *
     * @param value the return value to evaluate; {@code null} passes semantic
     *     contracts
     * @param methodName the fully qualified method name used in messages
     * @param annotations the annotations applied to the method
     * @throws IllegalStateException when a return-value contract fails
     */
    public static void requireReturn(@Nullable Object value, String methodName, Annotation... annotations) {
        ContractEvaluation evaluation = ContractAnnotations.evaluate(value, annotations);

        if (!evaluation.valid()) {
            throw ContractMessages.postconditionViolation(
                    methodName, evaluation.rule(), value, evaluation.maskRenderer());
        }
    }

    /**
     * Evaluates a generated parameter contract with pre-resolved metadata.
     *
     * <p>Generated code can use this lower-level entry point when it has already
     * resolved the runtime contract kind, message description, mask renderer,
     * and attribute values from source annotations.
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
     * Evaluates a generated return-value contract with pre-resolved metadata.
     *
     * <p>Generated code can use this lower-level entry point when it has already
     * resolved the runtime contract kind, message description, mask renderer,
     * and attribute values from source annotations. The original value is
     * returned unchanged when the contract passes so generated return statements
     * can remain expression-shaped.
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
     * Tests whether a supported value is non-empty.
     *
     * @param value value to evaluate; {@code null} passes
     * @return {@code true} when the value is {@code null} or has non-zero size
     */
    public static boolean isNotEmpty(@Nullable Object value) {
        return ContractChecks.isNotEmpty(value);
    }

    /**
     * Tests whether a character sequence contains non-whitespace content.
     *
     * @param value value to evaluate; {@code null} passes
     * @return {@code true} when the value is {@code null} or contains at least
     *     one non-whitespace character
     */
    public static boolean isNotBlank(@Nullable Object value) {
        return ContractChecks.isNotBlank(value);
    }

    /**
     * Tests whether a supported numeric value is greater than zero.
     *
     * @param value value to evaluate; {@code null} passes
     * @return {@code true} when the value is {@code null} or numerically
     *     greater than zero
     */
    public static boolean isPositive(@Nullable Object value) {
        return ContractChecks.isPositive(value);
    }

    /**
     * Tests whether a supported numeric value is less than zero.
     *
     * @param value value to evaluate; {@code null} passes
     * @return {@code true} when the value is {@code null} or numerically less
     *     than zero
     */
    public static boolean isNegative(@Nullable Object value) {
        return ContractChecks.isNegative(value);
    }

    /**
     * Tests whether a supported numeric value is greater than or equal to zero.
     *
     * @param value value to evaluate; {@code null} passes
     * @return {@code true} when the value is {@code null} or numerically
     *     greater than or equal to zero
     */
    public static boolean isNonNegative(@Nullable Object value) {
        return ContractChecks.isNonNegative(value);
    }

    /**
     * Tests whether a supported numeric value is less than or equal to zero.
     *
     * @param value value to evaluate; {@code null} passes
     * @return {@code true} when the value is {@code null} or numerically less
     *     than or equal to zero
     */
    public static boolean isNonPositive(@Nullable Object value) {
        return ContractChecks.isNonPositive(value);
    }

    /**
     * Tests whether a supported numeric value is within the configured bounds.
     *
     * @param value value to evaluate; {@code null} passes
     * @param min lower numeric bound
     * @param max upper numeric bound
     * @param minInclusive whether the lower bound is inclusive
     * @param maxInclusive whether the upper bound is inclusive
     * @return {@code true} when the value is {@code null} or falls within the
     *     configured range
     */
    public static boolean isInRange(
            @Nullable Object value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return ContractChecks.isInRange(value, min, max, minInclusive, maxInclusive);
    }

    /**
     * Tests whether a supported value has length or size within inclusive bounds.
     *
     * @param value value to evaluate; {@code null} passes
     * @param min inclusive minimum length or size
     * @param max inclusive maximum length or size
     * @return {@code true} when the value is {@code null} or has size within
     *     the configured range
     */
    public static boolean hasSize(@Nullable Object value, int min, int max) {
        return ContractChecks.hasSize(value, min, max);
    }

    /**
     * Tests whether a character sequence fully matches a regular expression.
     *
     * @param value value to evaluate; {@code null} passes
     * @param regexp Java regular expression
     * @return {@code true} when the value is {@code null} or the full sequence
     *     matches {@code regexp}
     */
    public static boolean matchesPattern(@Nullable Object value, String regexp) {
        return ContractChecks.matchesPattern(value, regexp);
    }

    /**
     * Renders a value for use in generated violation messages.
     *
     * @param value value to render
     * @param maskRenderer renderer type for confidential values, or
     *     {@code null} to render the value normally
     * @return rendered message value
     */
    public static String renderValue(@Nullable Object value, @Nullable Class<? extends MaskRenderer> maskRenderer) {
        return ValueRenderer.render(value, maskRenderer);
    }
}
