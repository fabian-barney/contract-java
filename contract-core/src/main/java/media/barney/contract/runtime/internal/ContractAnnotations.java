package media.barney.contract.runtime.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import media.barney.contract.Contract;
import media.barney.contract.MaskRenderer;

public final class ContractAnnotations {

    private ContractAnnotations() {}

    public static ContractEvaluation evaluate(Object value, Annotation[] annotations) {
        Class<? extends MaskRenderer> maskRenderer = findMaskRenderer(annotations);

        for (Annotation annotation : annotations) {
            ContractEvaluation evaluation =
                    evaluateAnnotation(value, annotation, Optional.empty(), maskRenderer, new HashSet<>());
            if (!evaluation.valid()) {
                return evaluation;
            }
        }

        return ContractEvaluation.valid(maskRenderer);
    }

    private static ContractEvaluation evaluateAnnotation(
            Object value,
            Annotation annotation,
            Optional<String> messageOverride,
            Class<? extends MaskRenderer> maskRenderer,
            Set<Class<? extends Annotation>> visited) {
        Optional<ContractRule> directRule = directRule(value, annotation, messageOverride);
        if (directRule.isPresent()) {
            return ContractEvaluation.invalid(directRule.orElseThrow(), maskRenderer);
        }
        if (!isComposedContract(annotation, visited)) {
            return ContractEvaluation.valid(maskRenderer);
        }

        Optional<String> nextOverride = messageOverride.or(() -> messageFrom(annotation));
        return evaluateMetaAnnotations(value, annotation, nextOverride, maskRenderer, visited);
    }

    private static ContractEvaluation evaluateMetaAnnotations(
            Object value,
            Annotation annotation,
            Optional<String> messageOverride,
            Class<? extends MaskRenderer> maskRenderer,
            Set<Class<? extends Annotation>> visited) {
        for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
            if (isFrameworkMetaAnnotation(metaAnnotation)) {
                continue;
            }

            ContractEvaluation evaluation =
                    evaluateAnnotation(value, metaAnnotation, messageOverride, maskRenderer, visited);
            if (!evaluation.valid()) {
                return evaluation;
            }
        }

        return ContractEvaluation.valid(maskRenderer);
    }

    private static Optional<ContractRule> directRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        return contentRule(value, annotation, messageOverride)
                .or(() -> numericRule(value, annotation, messageOverride))
                .or(() -> rangeRule(value, annotation, messageOverride))
                .or(() -> sizeRule(value, annotation, messageOverride))
                .or(() -> patternRule(value, annotation, messageOverride));
    }

    private static Optional<ContractRule> contentRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        return notEmptyRule(value, annotation, messageOverride)
                .or(() -> notBlankRule(value, annotation, messageOverride));
    }

    private static Optional<ContractRule> notEmptyRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.NotEmpty contract && !ContractChecks.isNotEmpty(value)) {
            return Optional.of(rule("must not be empty", contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> notBlankRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.NotBlank contract && !ContractChecks.isNotBlank(value)) {
            return Optional.of(rule("must not be blank", contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> numericRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        return positiveRule(value, annotation, messageOverride)
                .or(() -> negativeRule(value, annotation, messageOverride))
                .or(() -> nonNegativeRule(value, annotation, messageOverride))
                .or(() -> nonPositiveRule(value, annotation, messageOverride));
    }

    private static Optional<ContractRule> positiveRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.Positive contract && !ContractChecks.isPositive(value)) {
            return Optional.of(rule("must be positive", contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> negativeRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.Negative contract && !ContractChecks.isNegative(value)) {
            return Optional.of(rule("must be negative", contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> nonNegativeRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.NonNegative contract && !ContractChecks.isNonNegative(value)) {
            return Optional.of(rule("must be non-negative", contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> nonPositiveRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.NonPositive contract && !ContractChecks.isNonPositive(value)) {
            return Optional.of(rule("must be non-positive", contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> rangeRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.InRange contract
                && !ContractChecks.isInRange(
                        value, contract.min(), contract.max(), contract.minInclusive(), contract.maxInclusive())) {
            return Optional.of(rule(rangeDescription(contract), contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> sizeRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.Size contract
                && !ContractChecks.hasSize(value, contract.min(), contract.max())) {
            return Optional.of(rule(sizeDescription(contract), contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static Optional<ContractRule> patternRule(
            Object value, Annotation annotation, Optional<String> messageOverride) {
        if (annotation instanceof Contract.Pattern contract
                && !ContractChecks.matchesPattern(value, contract.regexp())) {
            return Optional.of(rule("must match the required pattern", contract.message(), messageOverride));
        }

        return Optional.empty();
    }

    private static ContractRule rule(
            String defaultDescription, String annotationMessage, Optional<String> messageOverride) {
        Optional<String> description = messageOverride.or(() -> nonBlank(annotationMessage));
        return description
                .map(message -> new ContractRule(message, true))
                .orElseGet(() -> new ContractRule(defaultDescription, false));
    }

    private static Class<? extends MaskRenderer> findMaskRenderer(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            Optional<Class<? extends MaskRenderer>> renderer = findMaskRenderer(annotation, new HashSet<>());
            if (renderer.isPresent()) {
                return renderer.orElseThrow();
            }
        }

        return null;
    }

    private static Optional<Class<? extends MaskRenderer>> findMaskRenderer(
            Annotation annotation, Set<Class<? extends Annotation>> visited) {
        if (annotation instanceof Contract.Mask mask) {
            return Optional.of(mask.renderer());
        }
        if (!isComposedContract(annotation, visited)) {
            return Optional.empty();
        }

        return findMaskRendererInMetaAnnotations(annotation.annotationType().getAnnotations(), visited);
    }

    private static Optional<Class<? extends MaskRenderer>> findMaskRendererInMetaAnnotations(
            Annotation[] annotations, Set<Class<? extends Annotation>> visited) {
        for (Annotation metaAnnotation : annotations) {
            if (!isFrameworkMetaAnnotation(metaAnnotation)) {
                Optional<Class<? extends MaskRenderer>> renderer = findMaskRenderer(metaAnnotation, visited);
                if (renderer.isPresent()) {
                    return renderer;
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isComposedContract(Annotation annotation, Set<Class<? extends Annotation>> visited) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        return !isBuiltIn(annotation)
                && annotationType.isAnnotationPresent(Contract.class)
                && visited.add(annotationType);
    }

    private static boolean isBuiltIn(Annotation annotation) {
        return annotation.annotationType().getName().startsWith(Contract.class.getName() + "$");
    }

    private static boolean isFrameworkMetaAnnotation(Annotation annotation) {
        return annotation instanceof Contract
                || annotation.annotationType().getName().startsWith("java.lang.annotation.");
    }

    private static Optional<String> messageFrom(Annotation annotation) {
        try {
            Method message = annotation.annotationType().getDeclaredMethod("message");
            return messageValue(message, annotation)
                    .flatMap(value -> value instanceof String text ? nonBlank(text) : Optional.empty());
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> messageValue(Method message, Annotation annotation)
            throws ReflectiveOperationException {
        try {
            return Optional.ofNullable(message.invoke(annotation));
        } catch (IllegalAccessException exception) {
            if (!message.trySetAccessible()) {
                return Optional.empty();
            }

            return Optional.ofNullable(message.invoke(annotation));
        }
    }

    private static Optional<String> nonBlank(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    private static String rangeDescription(Contract.InRange contract) {
        return "must be within "
                + lowerSymbol(contract.minInclusive())
                + contract.min()
                + ", "
                + contract.max()
                + upperSymbol(contract.maxInclusive());
    }

    private static String sizeDescription(Contract.Size contract) {
        return "must have size within [" + contract.min() + ", " + contract.max() + "]";
    }

    private static String lowerSymbol(boolean inclusive) {
        return inclusive ? "[" : "(";
    }

    private static String upperSymbol(boolean inclusive) {
        return inclusive ? "]" : ")";
    }
}
