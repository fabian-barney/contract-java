package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import media.barney.contract.Contract;
import media.barney.contract.DefaultMaskRenderer;
import media.barney.contract.MaskRenderer;
import org.junit.jupiter.api.Test;

class MaskContractTest {

    @Test
    void nullValueIsMasked() {
        assertEquals("[MASKED]", ValueRenderer.render(null, DefaultMaskRenderer.class));
    }

    @Test
    void stringValueIsMasked() {
        assertEquals("[MASKED]", ValueRenderer.render("secret-value", DefaultMaskRenderer.class));
    }

    @Test
    void numericValueIsMasked() {
        assertEquals("[MASKED]", ValueRenderer.render(12345, DefaultMaskRenderer.class));
        assertEquals("[MASKED]", ValueRenderer.render(99.99, DefaultMaskRenderer.class));
    }

    @Test
    void booleanValueIsMasked() {
        assertEquals("[MASKED]", ValueRenderer.render(true, DefaultMaskRenderer.class));
        assertEquals("[MASKED]", ValueRenderer.render(false, DefaultMaskRenderer.class));
    }

    @Test
    void customMaskRendererIsApplied() {
        assertEquals("[text]", ValueRenderer.render("secret", TextMaskRenderer.class));
    }

    @Test
    void customMaskRendererReceivesOriginalValue() {
        assertEquals("length:5", ValueRenderer.render("hello", LengthMaskRenderer.class));
    }

    @Test
    void throwingMaskRendererFallsBackToDefault() {
        String result = ValueRenderer.render("secret", ThrowingMaskRenderer.class);
        assertEquals("[MASKED]", result);
    }

    @Test
    void linkageErrorMaskRendererFallsBackToDefault() {
        String result = ValueRenderer.render("secret", LinkageErrorMaskRenderer.class);
        assertEquals("[MASKED]", result);
    }

    @Test
    void maskRendererDoesNotExposeRawValue() {
        String secret = "super-secret-password-123";
        String masked = ValueRenderer.render(secret, DefaultMaskRenderer.class);
        
        assertFalse(masked.contains("secret"));
        assertFalse(masked.contains("password"));
        assertFalse(masked.contains("123"));
        assertEquals("[MASKED]", masked);
    }

    @Test
    void differentValuesProduceSameMaskedOutput() {
        String masked1 = ValueRenderer.render("value1", DefaultMaskRenderer.class);
        String masked2 = ValueRenderer.render("value2", DefaultMaskRenderer.class);
        String masked3 = ValueRenderer.render("completely-different", DefaultMaskRenderer.class);
        
        assertEquals(masked1, masked2);
        assertEquals(masked2, masked3);
        assertEquals("[MASKED]", masked1);
    }

    @Test
    void maskWithParameterValidationIncludesMaskedValue() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        "",
                        "com.example.Service.authenticate",
                        "token",
                        parameterAnnotations("maskedToken", String.class)));

        assertTrue(exception.getMessage().contains("Parameter 'token'"));
        assertTrue(exception.getMessage().contains("[MASKED]"));
        assertFalse(exception.getMessage().contains("secret-token"));
    }

    @Test
    void maskWithReturnValidationIncludesMaskedValue() {
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> ContractRuntime.requireReturn(
                        "api-key-12345",
                        "com.example.Service.getApiKey",
                        methodAnnotations("maskedPattern")));

        assertTrue(exception.getMessage().contains("Postcondition"));
        assertTrue(exception.getMessage().contains("[MASKED]"));
        assertFalse(exception.getMessage().contains("api-key-12345"));
    }

    @Test
    void customMessageWithMaskWorks() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ContractRuntime.requireParameter(
                        "",
                        "com.example.Service.process",
                        "data",
                        parameterAnnotations("customMaskedMessage", String.class)));

        assertTrue(exception.getMessage().contains("confidential information required"));
        assertTrue(exception.getMessage().contains("[text]"));
        assertFalse(exception.getMessage().contains("confidential-data"));
    }

    private static Annotation[] parameterAnnotations(String methodName, Class<?> parameterType) {
        try {
            return Fixture.class.getDeclaredMethod(methodName, parameterType).getParameterAnnotations()[0];
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Annotation[] methodAnnotations(String methodName) {
        try {
            return Fixture.class.getDeclaredMethod(methodName).getAnnotations();
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings({"unused", "PMD.UnusedPrivateMethod"})
    private static final class Fixture {

        void maskedToken(@Contract.Mask @Contract.NotBlank String token) {
        }

        void customMaskedMessage(@CustomMasked String data) {
        }

        @Contract.Mask
        @Contract.Pattern(regexp = "[a-z]+")
        String maskedPattern() {
            return "";
        }
    }

    @Contract
    @Contract.Mask(renderer = TextMaskRenderer.class)
    @Contract.Pattern(regexp = ".+")
    @java.lang.annotation.Target({java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    private @interface CustomMasked {
        String message() default "confidential information required";
    }

    public static final class TextMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            return "[text]";
        }
    }

    public static final class LengthMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            if (value == null) {
                return "length:0";
            }
            return "length:" + value.toString().length();
        }
    }

    public static final class ThrowingMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new RuntimeException("renderer failed");
        }
    }

    public static final class LinkageErrorMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new LinkageError("renderer linkage error");
        }
    }
}
