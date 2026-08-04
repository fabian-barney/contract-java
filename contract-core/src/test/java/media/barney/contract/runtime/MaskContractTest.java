package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MaskContractTest {

    @Test
    void defaultMaskingSuppressesRawValues() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ContractRuntime.requireReturn(
                        "",
                        "com.example.TokenService.issue",
                        BuiltInContractTestSupport.methodAnnotations("maskedNotBlank")));

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
                () -> ContractRuntime.requireParameter(
                        "secret-token",
                        "com.example.AccountService.login",
                        "password",
                        BuiltInContractTestSupport.parameterAnnotations("sensitivePattern", String.class)));

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
                () -> ContractRuntime.requireReturn(
                        "raw-secret",
                        "com.example.TokenService.issue",
                        BuiltInContractTestSupport.methodAnnotations("throwingMask")));
        LinkageError linkageMask = assertThrows(
                LinkageError.class,
                () -> ContractRuntime.requireReturn(
                        "raw-secret",
                        "com.example.TokenService.issue",
                        BuiltInContractTestSupport.methodAnnotations("linkageMask")));

        assertEquals(
                "Postcondition of method 'com.example.TokenService.issue' violated: "
                        + "return value must match the required pattern, but was: [MASKED]",
                throwingMask.getMessage());
        assertEquals("renderer linkage failed", linkageMask.getMessage());
        assertFalse(throwingMask.getMessage().contains("raw-secret"));
    }
}
