package media.barney.contract.processor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import media.barney.contract.Contract;
import media.barney.contract.MaskRenderer;
import media.barney.contract.runtime.RuntimeContract;

final class ContractResolver {

    private static final String CONTRACT = "media.barney.contract.Contract";
    private static final String MASK = "media.barney.contract.Contract.Mask";
    private static final String NOT_EMPTY = "media.barney.contract.Contract.NotEmpty";
    private static final String NOT_BLANK = "media.barney.contract.Contract.NotBlank";
    private static final String POSITIVE = "media.barney.contract.Contract.Positive";
    private static final String NEGATIVE = "media.barney.contract.Contract.Negative";
    private static final String NON_NEGATIVE = "media.barney.contract.Contract.NonNegative";
    private static final String NON_POSITIVE = "media.barney.contract.Contract.NonPositive";
    private static final String IN_RANGE = "media.barney.contract.Contract.InRange";
    private static final String SIZE = "media.barney.contract.Contract.Size";
    private static final String PATTERN = "media.barney.contract.Contract.Pattern";
    private static final Map<String, ContractFactory> BUILT_INS = Map.ofEntries(
            Map.entry(NOT_EMPTY, simple(RuntimeContract.NOT_EMPTY, "must not be empty")),
            Map.entry(NOT_BLANK, simple(RuntimeContract.NOT_BLANK, "must not be blank")),
            Map.entry(POSITIVE, simple(RuntimeContract.POSITIVE, "must be positive")),
            Map.entry(NEGATIVE, simple(RuntimeContract.NEGATIVE, "must be negative")),
            Map.entry(NON_NEGATIVE, simple(RuntimeContract.NON_NEGATIVE, "must be non-negative")),
            Map.entry(NON_POSITIVE, simple(RuntimeContract.NON_POSITIVE, "must be non-positive")),
            Map.entry(IN_RANGE, ContractResolver::rangeContract),
            Map.entry(SIZE, ContractResolver::sizeContract),
            Map.entry(PATTERN, ContractResolver::patternContract));

    private final Elements elements;
    private final Types types;

    ContractResolver(Elements elements, Types types) {
        this.elements = elements;
        this.types = types;
    }

    List<ProcessorContract> semanticContracts(Element element) {
        List<ProcessorContract> contracts = new ArrayList<>();
        String maskRenderer = maskRenderer(element.getAnnotationMirrors()).orElse(null);

        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            collectContracts(annotation, Optional.empty(), maskRenderer, new HashSet<>(), contracts);
        }

        return contracts;
    }

    boolean supports(ProcessorContract contract, TypeMirror valueType) {
        return switch (contract.kind()) {
            case NOT_EMPTY, SIZE -> supportsSize(valueType);
            case NOT_BLANK, PATTERN -> isAssignableTo(valueType, CharSequence.class.getCanonicalName());
            case POSITIVE, NEGATIVE, NON_NEGATIVE, NON_POSITIVE, IN_RANGE -> supportsNumeric(valueType);
        };
    }

    private void collectContracts(
            AnnotationMirror annotation,
            Optional<String> messageOverride,
            String maskRenderer,
            Set<String> visited,
            List<ProcessorContract> contracts) {
        Optional<ProcessorContract> builtIn = builtInContract(annotation, messageOverride, maskRenderer);
        if (builtIn.isPresent()) {
            contracts.add(builtIn.orElseThrow());
            return;
        }

        Element annotationElement = annotation.getAnnotationType().asElement();
        String annotationName = annotationName(annotation);
        if (!isComposedContract(annotationElement, annotationName, visited)) {
            return;
        }

        Optional<String> nextOverride = messageOverride.or(() -> message(annotation));
        for (AnnotationMirror metaAnnotation : annotationElement.getAnnotationMirrors()) {
            if (!isFrameworkMetaAnnotation(metaAnnotation)) {
                collectContracts(metaAnnotation, nextOverride, maskRenderer, visited, contracts);
            }
        }
    }

    private Optional<ProcessorContract> builtInContract(
            AnnotationMirror annotation, Optional<String> messageOverride, String maskRenderer) {
        Optional<String> message = messageOverride.or(() -> message(annotation));
        ContractFactory factory = BUILT_INS.get(annotationName(annotation));

        return factory == null
                ? Optional.empty()
                : Optional.of(factory.create(this, annotation, message, maskRenderer));
    }

    private Optional<String> maskRenderer(List<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror annotation : annotations) {
            Optional<String> renderer = maskRenderer(annotation, new HashSet<>());
            if (renderer.isPresent()) {
                return renderer;
            }
        }

        return Optional.empty();
    }

    private Optional<String> maskRenderer(AnnotationMirror annotation, Set<String> visited) {
        if (MASK.equals(annotationName(annotation))) {
            return Optional.of(typeValue(annotation, "renderer", MaskRenderer.class.getCanonicalName()));
        }

        Element annotationElement = annotation.getAnnotationType().asElement();
        String annotationName = annotationName(annotation);
        if (!isComposedContract(annotationElement, annotationName, visited)) {
            return Optional.empty();
        }

        for (AnnotationMirror metaAnnotation : annotationElement.getAnnotationMirrors()) {
            if (!isFrameworkMetaAnnotation(metaAnnotation)) {
                Optional<String> renderer = maskRenderer(metaAnnotation, visited);
                if (renderer.isPresent()) {
                    return renderer;
                }
            }
        }

        return Optional.empty();
    }

    private ProcessorContract rangeContract(
            AnnotationMirror annotation, Optional<String> message, String maskRenderer) {
        long min = longValue(annotation, "min", 0L);
        long max = longValue(annotation, "max", 0L);
        boolean minInclusive = booleanValue(annotation, "minInclusive", true);
        boolean maxInclusive = booleanValue(annotation, "maxInclusive", true);
        String defaultDescription =
                "must be within " + (minInclusive ? "[" : "(") + min + ", " + max + (maxInclusive ? "]" : ")");

        return new ProcessorContract(
                RuntimeContract.IN_RANGE,
                message.orElse(defaultDescription),
                message.isPresent(),
                maskRenderer,
                min,
                max,
                minInclusive,
                maxInclusive,
                0,
                0,
                null);
    }

    private ProcessorContract sizeContract(AnnotationMirror annotation, Optional<String> message, String maskRenderer) {
        int min = intValue(annotation, "min", 0);
        int max = intValue(annotation, "max", Integer.MAX_VALUE);

        return new ProcessorContract(
                RuntimeContract.SIZE,
                message.orElse("must have size within [" + min + ", " + max + "]"),
                message.isPresent(),
                maskRenderer,
                0L,
                0L,
                true,
                true,
                min,
                max,
                null);
    }

    private ProcessorContract patternContract(
            AnnotationMirror annotation, Optional<String> message, String maskRenderer) {
        return new ProcessorContract(
                RuntimeContract.PATTERN,
                message.orElse("must match the required pattern"),
                message.isPresent(),
                maskRenderer,
                0L,
                0L,
                true,
                true,
                0,
                0,
                stringValue(annotation, "regexp", ""));
    }

    private static ProcessorContract contract(
            RuntimeContract kind, String defaultDescription, Optional<String> message, String maskRenderer) {
        return new ProcessorContract(
                kind,
                message.orElse(defaultDescription),
                message.isPresent(),
                maskRenderer,
                0L,
                0L,
                true,
                true,
                0,
                0,
                null);
    }

    private static ContractFactory simple(RuntimeContract kind, String defaultDescription) {
        return (resolver, annotation, message, maskRenderer) ->
                contract(kind, defaultDescription, message, maskRenderer);
    }

    private boolean supportsSize(TypeMirror valueType) {
        return valueType instanceof ArrayType
                || isAssignableTo(valueType, CharSequence.class.getCanonicalName())
                || isAssignableTo(valueType, java.util.Collection.class.getCanonicalName())
                || isAssignableTo(valueType, java.util.Map.class.getCanonicalName());
    }

    private boolean supportsNumeric(TypeMirror valueType) {
        if (valueType.getKind().isPrimitive()) {
            return switch (valueType.getKind()) {
                case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true;
                default -> false;
            };
        }

        return isAssignableTo(valueType, Number.class.getCanonicalName());
    }

    private boolean isAssignableTo(TypeMirror valueType, String targetType) {
        TypeElement targetElement = elements.getTypeElement(targetType);
        if (targetElement == null) {
            return false;
        }

        return types.isAssignable(types.erasure(valueType), types.erasure(targetElement.asType()));
    }

    private boolean isComposedContract(Element annotationElement, String annotationName, Set<String> visited) {
        return !isBuiltIn(annotationName)
                && annotationElement.getAnnotation(Contract.class) != null
                && visited.add(annotationName);
    }

    private static boolean isBuiltIn(String annotationName) {
        return annotationName.startsWith(CONTRACT + ".");
    }

    private static boolean isFrameworkMetaAnnotation(AnnotationMirror annotation) {
        String annotationName = annotationName(annotation);
        return CONTRACT.equals(annotationName) || annotationName.startsWith("java.lang.annotation.");
    }

    private Optional<String> message(AnnotationMirror annotation) {
        String message = stringValue(annotation, "message", "");
        return message.isBlank() ? Optional.empty() : Optional.of(message);
    }

    private String stringValue(AnnotationMirror annotation, String name, String defaultValue) {
        Object value = value(annotation, name).orElse(defaultValue);
        return value instanceof String text ? text : defaultValue;
    }

    private int intValue(AnnotationMirror annotation, String name, int defaultValue) {
        Object value = value(annotation, name).orElse(defaultValue);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private long longValue(AnnotationMirror annotation, String name, long defaultValue) {
        Object value = value(annotation, name).orElse(defaultValue);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }

    private boolean booleanValue(AnnotationMirror annotation, String name, boolean defaultValue) {
        Object value = value(annotation, name).orElse(defaultValue);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }

    private String typeValue(AnnotationMirror annotation, String name, String defaultValue) {
        Object value = value(annotation, name).orElse(defaultValue);
        return value instanceof TypeMirror type ? type.toString() : defaultValue;
    }

    private Optional<Object> value(AnnotationMirror annotation, String name) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                elements.getElementValuesWithDefaults(annotation);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(name)) {
                return Optional.ofNullable(entry.getValue().getValue());
            }
        }

        return Optional.empty();
    }

    private static String annotationName(AnnotationMirror annotation) {
        return annotation.getAnnotationType().toString();
    }

    @FunctionalInterface
    private interface ContractFactory {

        ProcessorContract create(
                ContractResolver resolver, AnnotationMirror annotation, Optional<String> message, String maskRenderer);
    }
}
