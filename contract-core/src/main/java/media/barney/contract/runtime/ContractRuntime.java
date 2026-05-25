package media.barney.contract.runtime;

import java.lang.annotation.Annotation;
import media.barney.contract.MaskRenderer;

/**
 * Runtime support used by generated contract checks.
 */
public final class ContractRuntime {

    private ContractRuntime() {
        throw new AssertionError("No instances.");
    }

    public static void requireParameter(
            Object value, String methodName, String parameterName, Annotation... annotations) {
        ContractEvaluation evaluation = ContractAnnotations.evaluate(value, annotations);

        if (!evaluation.valid()) {
            throw ContractMessages.preconditionViolation(
                    methodName, parameterName, evaluation.rule(), value, evaluation.maskRenderer());
        }
    }

    public static void requireReturn(Object value, String methodName, Annotation... annotations) {
        ContractEvaluation evaluation = ContractAnnotations.evaluate(value, annotations);

        if (!evaluation.valid()) {
            throw ContractMessages.postconditionViolation(
                    methodName, evaluation.rule(), value, evaluation.maskRenderer());
        }
    }

    public static void requireParameterValue(
            Object value,
            String methodName,
            String parameterName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.preconditionViolation(methodName, parameterName, rule, value, maskRenderer);
        }
    }

    public static <T> T requireReturnValue(
            T value,
            String methodName,
            RuntimeContract contract,
            String description,
            boolean customDescription,
            Class<? extends MaskRenderer> maskRenderer,
            long min,
            long max,
            boolean minInclusive,
            boolean maxInclusive,
            int sizeMin,
            int sizeMax,
            String regexp) {
        ContractRule rule = new ContractRule(description, customDescription);
        ContractArguments arguments =
                new ContractArguments(min, max, minInclusive, maxInclusive, sizeMin, sizeMax, regexp);

        if (!contract.isValid(value, arguments)) {
            throw ContractMessages.postconditionViolation(methodName, rule, value, maskRenderer);
        }

        return value;
    }

    public static boolean isNotEmpty(Object value) {
        return ContractChecks.isNotEmpty(value);
    }

    public static boolean isNotBlank(Object value) {
        return ContractChecks.isNotBlank(value);
    }

    public static boolean isPositive(Object value) {
        return ContractChecks.isPositive(value);
    }

    public static boolean isNegative(Object value) {
        return ContractChecks.isNegative(value);
    }

    public static boolean isNonNegative(Object value) {
        return ContractChecks.isNonNegative(value);
    }

    public static boolean isNonPositive(Object value) {
        return ContractChecks.isNonPositive(value);
    }

    public static boolean isInRange(Object value, long min, long max, boolean minInclusive, boolean maxInclusive) {
        return ContractChecks.isInRange(value, min, max, minInclusive, maxInclusive);
    }

    public static boolean hasSize(Object value, int min, int max) {
        return ContractChecks.hasSize(value, min, max);
    }

    public static boolean matchesPattern(Object value, String regexp) {
        return ContractChecks.matchesPattern(value, regexp);
    }

    public static String renderValue(Object value, Class<? extends MaskRenderer> maskRenderer) {
        return ValueRenderer.render(value, maskRenderer);
    }
}
