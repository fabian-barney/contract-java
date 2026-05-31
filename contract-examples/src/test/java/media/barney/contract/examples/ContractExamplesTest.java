package media.barney.contract.examples;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import media.barney.contract.Contract;
import org.junit.jupiter.api.Test;

class ContractExamplesTest {

    private final AccountRegistration registration = new AccountRegistration();

    @Test
    void builtInContractsAllowValidInput() {
        assertEquals("acct-42", registration.register("tenant-a", "correct-horse-battery-staple", 42L));
    }

    @Test
    void builtInContractsRejectInvalidInput() {
        Throwable violation = assertThrows(
                IllegalArgumentException.class, () -> registration.register("", "correct-horse-battery-staple", 42L));

        assertTrue(violation.getMessage().contains("tenant"));
    }

    @Test
    void customComposedContractRejectsInvalidInput() {
        Throwable violation = assertThrows(
                IllegalArgumentException.class,
                () -> registration.register("tenant-a", "correct-horse-battery-staple", 0L));

        assertTrue(violation.getMessage().contains("customer id must be positive"));
    }

    @Test
    void maskedValuesDoNotLeakInViolationMessages() {
        String secret = "short";

        Throwable violation =
                assertThrows(IllegalArgumentException.class, () -> registration.register("tenant-a", secret, 42L));

        assertAll(
                () -> assertTrue(violation.getMessage().contains("[MASKED]")),
                () -> assertFalse(violation.getMessage().contains(secret)));
    }

    @Test
    void returnContractsReportPostconditionFailures() {
        Throwable violation = assertThrows(IllegalStateException.class, registration::brokenAccountReference);

        assertInstanceOf(IllegalStateException.class, violation);
    }

    private static final class AccountRegistration {

        @Contract.Pattern(regexp = "acct-[0-9]+") String register(
                @Contract.NotBlank String tenant,
                @Contract.Mask @Contract.Size(min = 8, max = 64) String accessToken,
                @CustomerId long customerId) {
            return "acct-" + customerId;
        }

        @Contract.Pattern(regexp = "acct-[0-9]+") String brokenAccountReference() {
            return "temporary-reference";
        }
    }

    @Contract
    @Contract.Positive(message = "customer id must be positive") @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    private @interface CustomerId {}
}
