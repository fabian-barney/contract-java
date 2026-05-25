package media.barney.contract;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ContractAnnotationApiTest {

    private static final Set<Class<? extends Annotation>> FAILING_CONTRACTS = Set.of(
            Contract.NotEmpty.class,
            Contract.NotBlank.class,
            Contract.Positive.class,
            Contract.Negative.class,
            Contract.NonNegative.class,
            Contract.NonPositive.class,
            Contract.InRange.class,
            Contract.Size.class,
            Contract.Pattern.class);

    @Test
    void contractIsRuntimeMetaAnnotation() {
        assertArrayEquals(
                new java.lang.annotation.ElementType[] {ANNOTATION_TYPE},
                Contract.class.getAnnotation(Target.class).value());
        assertEquals(RUNTIME, Contract.class.getAnnotation(Retention.class).value());
        assertTrue(Contract.class.isAnnotationPresent(Documented.class));
    }

    @Test
    void builtInContractsSupportAllPublicPlacements() {
        Set<java.lang.annotation.ElementType> expected = Set.of(PARAMETER, METHOD, FIELD, ANNOTATION_TYPE);

        for (Class<? extends Annotation> annotationType : allBuiltInAnnotations()) {
            Set<java.lang.annotation.ElementType> actual = Arrays.stream(
                            annotationType.getAnnotation(Target.class).value())
                    .collect(Collectors.toUnmodifiableSet());

            assertEquals(expected, actual, annotationType.getName());
            assertEquals(RUNTIME, annotationType.getAnnotation(Retention.class).value());
            assertTrue(annotationType.isAnnotationPresent(Documented.class), annotationType.getName());
        }
    }

    @Test
    void builtInContractsAreContractMetaAnnotations() {
        for (Class<? extends Annotation> annotationType : allBuiltInAnnotations()) {
            assertTrue(annotationType.isAnnotationPresent(Contract.class), annotationType.getName());
        }
    }

    @Test
    void failingBuiltInsExposeMessageAttribute() throws Exception {
        for (Class<? extends Annotation> annotationType : FAILING_CONTRACTS) {
            Method message = annotationType.getDeclaredMethod("message");

            assertEquals(String.class, message.getReturnType(), annotationType.getName());
            assertEquals("", message.getDefaultValue(), annotationType.getName());
        }
    }

    @Test
    void rangeDefaultsAreInclusive() throws Exception {
        assertEquals(
                true, Contract.InRange.class.getDeclaredMethod("minInclusive").getDefaultValue());
        assertEquals(
                true, Contract.InRange.class.getDeclaredMethod("maxInclusive").getDefaultValue());
    }

    @Test
    void sizeDefaultsAllowAnyNonNegativeSize() throws Exception {
        assertEquals(0, Contract.Size.class.getDeclaredMethod("min").getDefaultValue());
        assertEquals(
                Integer.MAX_VALUE, Contract.Size.class.getDeclaredMethod("max").getDefaultValue());
    }

    @Test
    void maskDefaultsToConservativeRenderer() throws Exception {
        Method renderer = Contract.Mask.class.getDeclaredMethod("renderer");

        assertEquals(DefaultMaskRenderer.class, renderer.getDefaultValue());
    }

    @Test
    void customAnnotationCanComposeBuiltIns() {
        assertTrue(ValidIdentifier.class.isAnnotationPresent(Contract.class));
        assertTrue(ValidIdentifier.class.isAnnotationPresent(Contract.Positive.class));
    }

    private static Set<Class<? extends Annotation>> allBuiltInAnnotations() {
        return Set.of(
                Contract.Mask.class,
                Contract.NotEmpty.class,
                Contract.NotBlank.class,
                Contract.Positive.class,
                Contract.Negative.class,
                Contract.NonNegative.class,
                Contract.NonPositive.class,
                Contract.InRange.class,
                Contract.Size.class,
                Contract.Pattern.class);
    }

    @Contract
    @Contract.Positive @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    private @interface ValidIdentifier {}
}
