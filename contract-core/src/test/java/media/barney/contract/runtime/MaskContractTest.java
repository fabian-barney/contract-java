package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import media.barney.contract.DefaultMaskRenderer;
import org.junit.jupiter.api.Test;

class MaskContractTest {

    @Test
    void defaultMaskingSuppressesRawValues() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> GeneratedCheck.notBlank()
                        .masked(DefaultMaskRenderer.class)
                        .requireReturn("", "com.example.TokenService.issue"));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must not be blank, but was: [MASKED]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("\"\""));
    }

    @Test
    void customMaskRendererAppliesCustomRepresentationWithoutLeakingRawValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> GeneratedCheck.pattern("[0-9]+")
                        .masked(BuiltInContractTestSupport.TextMaskRenderer.class)
                        .requireParameter("secret-token", "com.example.AccountService.login", "password"));

        assertEquals(
                "Parameter 'password' of method 'com.example.AccountService.login' "
                        + "must match the required pattern, but was: [text]",
                exception.getMessage());
        assertFalse(exception.getMessage().contains("secret-token"));
    }

    @Test
    void maskRendererExceptionsFallBackButErrorsPropagate() {
        IllegalStateException throwingMask = assertThrows(
                IllegalStateException.class,
                () -> GeneratedCheck.pattern("[0-9]+")
                        .masked(BuiltInContractTestSupport.ThrowingMaskRenderer.class)
                        .requireReturn("raw-secret", "com.example.TokenService.issue"));
        LinkageError linkageMask = assertThrows(
                LinkageError.class,
                () -> GeneratedCheck.pattern("[0-9]+")
                        .masked(BuiltInContractTestSupport.LinkageErrorMaskRenderer.class)
                        .requireReturn("raw-secret", "com.example.TokenService.issue"));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must match the required pattern, but was: [MASKED]",
                throwingMask.getMessage());
        assertEquals("renderer linkage failed", linkageMask.getMessage());
        assertFalse(throwingMask.getMessage().contains("raw-secret"));
    }
}
