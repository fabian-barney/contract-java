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
 */
@API(status = API.Status.MAINTAINED)
public final class ContractRuntime {

    private ContractRuntime() {
        throw new AssertionError("No instances.");
    }

    public static void requireParameter(
            @Nullable Object value, String methodName, String parameterName, Annotation... annotations) {
        ContractEvaluation evaluation = ContractAnnotations.evaluate(value, annotations);

        if (!evaluation.valid()) {
            throw ContractMessages.preconditionViolation(
                    methodName, parameterName, evaluation.rule(), value, evaluation.maskRenderer());
        }
    }

    public static void requireReturn(@Nullable Object value, String methodName, Annotation... annotations) {
        ContractEvaluation evaluation = ContractAnnotations.evaluate(value, annotations);

        if (!evaluation.valid()) {
            throw ContractMessages.postconditionViolation(
                    methodName, evaluation.rule(), value, evaluation.maskRenderer());
        }
    }

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

    public static boolean isNotEmpty(@Nullable Object value) {
        return ContractChecks.isNotEmpty(value);
    }

    public static boolean isNotBlank(@Nullable Object value) {
        return ContractChecks.isNotBlank(value);
    }

    public static boolean isPositive(@Nullable Object value) {
        return ContractChecks.isPositive(value);
    }

    public static boolean isNegative(@Nullable Object value) {
        return ContractChecks.isNegative(value);
    }

    public static boolean isNonNegative(@Nullable Object value) {
        return ContractChecks.isNonNegative(value);
    }

    public static boolean isNonPositive(@Nullable Object value) {
        return ContractChecks.isNonPositive(value);
    }

    public static boolean isInRange(
            @Nullable Object value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return ContractChecks.isInRange(value, min, max, minInclusive, maxInclusive);
    }

    public static boolean hasSize(@Nullable Object value, int min, int max) {
        return ContractChecks.hasSize(value, min, max);
    }

    public static boolean matchesPattern(@Nullable Object value, String regexp) {
        return ContractChecks.matchesPattern(value, regexp);
    }

    public static String renderValue(@Nullable Object value, @Nullable Class<? extends MaskRenderer> maskRenderer) {
        return ValueRenderer.render(value, maskRenderer);
    }
}
