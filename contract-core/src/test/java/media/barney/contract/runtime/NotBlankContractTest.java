package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NotBlankContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.isNotBlank(null));
    }

    @Test
    void emptyStringFails() {
        assertFalse(ContractRuntime.isNotBlank(""));
    }

    @Test
    void blankStringsFail() {
        assertFalse(ContractRuntime.isNotBlank(" "));
        assertFalse(ContractRuntime.isNotBlank("  "));
        assertFalse(ContractRuntime.isNotBlank("\t"));
        assertFalse(ContractRuntime.isNotBlank("\n"));
        assertFalse(ContractRuntime.isNotBlank("\r"));
        assertFalse(ContractRuntime.isNotBlank("\f"));
    }

    @Test
    void mixedWhitespaceStringsFail() {
        assertFalse(ContractRuntime.isNotBlank(" \t\n\r "));
        assertFalse(ContractRuntime.isNotBlank("\t\t\t"));
        assertFalse(ContractRuntime.isNotBlank("   \t   "));
    }

    @Test
    void nonBlankStringsPass() {
        assertTrue(ContractRuntime.isNotBlank("a"));
        assertTrue(ContractRuntime.isNotBlank("abc"));
        assertTrue(ContractRuntime.isNotBlank(" a"));
        assertTrue(ContractRuntime.isNotBlank("a "));
        assertTrue(ContractRuntime.isNotBlank(" a "));
    }

    @Test
    void stringWithInternalWhitespacePasses() {
        assertTrue(ContractRuntime.isNotBlank("a b"));
        assertTrue(ContractRuntime.isNotBlank("hello world"));
        assertTrue(ContractRuntime.isNotBlank("a\tb"));
        assertTrue(ContractRuntime.isNotBlank("a\nb"));
    }

    @Test
    void singleNonWhitespaceCharacterPasses() {
        assertTrue(ContractRuntime.isNotBlank("x"));
        assertTrue(ContractRuntime.isNotBlank("1"));
        assertTrue(ContractRuntime.isNotBlank("!"));
        assertTrue(ContractRuntime.isNotBlank("中"));
    }

    @Test
    void leadingAndTrailingWhitespaceWithContentPasses() {
        assertTrue(ContractRuntime.isNotBlank("  abc  "));
        assertTrue(ContractRuntime.isNotBlank("\t\nabc\r\n"));
        assertTrue(ContractRuntime.isNotBlank("    x    "));
    }

    @Test
    void unicodeWhitespaceBehavior() {
        // \u00A0 (NO-BREAK SPACE) is NOT considered whitespace by Character.isWhitespace()
        assertTrue(ContractRuntime.isNotBlank("\u00A0"));
        
        // Some Unicode spaces ARE recognized as whitespace
        // The exact set depends on Java's Character.isWhitespace() implementation
        // Test a few common ones to document behavior
        boolean enQuadIsBlank = !ContractRuntime.isNotBlank("\u2000");
        boolean ideographicIsBlank = !ContractRuntime.isNotBlank("\u3000");
        
        // At least one of these should be recognized as whitespace
        // This documents that Java recognizes some Unicode whitespace beyond ASCII
        assertTrue(enQuadIsBlank || ideographicIsBlank, 
            "At least some Unicode whitespace characters should be recognized");
    }

    @Test
    void standardWhitespaceFails() {
        // These ARE recognized as whitespace by Character.isWhitespace()
        assertFalse(ContractRuntime.isNotBlank(" "));
        assertFalse(ContractRuntime.isNotBlank("\t"));
        assertFalse(ContractRuntime.isNotBlank("\n"));
        assertFalse(ContractRuntime.isNotBlank("\r"));
        assertFalse(ContractRuntime.isNotBlank("\f"));
    }

    @Test
    void unicodeContentPasses() {
        assertTrue(ContractRuntime.isNotBlank("你好"));
        assertTrue(ContractRuntime.isNotBlank("こんにちは"));
        assertTrue(ContractRuntime.isNotBlank("안녕하세요"));
    }

    @Test
    void specialCharactersPass() {
        assertTrue(ContractRuntime.isNotBlank("@#$%^&*()"));
        assertTrue(ContractRuntime.isNotBlank("!@#"));
        assertTrue(ContractRuntime.isNotBlank("..."));
    }

    @Test
    void numbersPass() {
        assertTrue(ContractRuntime.isNotBlank("123"));
        assertTrue(ContractRuntime.isNotBlank("0"));
        assertTrue(ContractRuntime.isNotBlank("-1"));
        assertTrue(ContractRuntime.isNotBlank("3.14"));
    }

    @Test
    void nonCharSequenceTypesFail() {
        assertFalse(ContractRuntime.isNotBlank(123));
        assertFalse(ContractRuntime.isNotBlank(true));
        assertFalse(ContractRuntime.isNotBlank(new Object()));
    }

    @Test
    void stringBuilderBlankFails() {
        assertFalse(ContractRuntime.isNotBlank(new StringBuilder("")));
        assertFalse(ContractRuntime.isNotBlank(new StringBuilder("   ")));
    }

    @Test
    void stringBuilderNonBlankPasses() {
        assertTrue(ContractRuntime.isNotBlank(new StringBuilder("a")));
        assertTrue(ContractRuntime.isNotBlank(new StringBuilder(" a ")));
    }

    @Test
    void stringBufferBlankFails() {
        assertFalse(ContractRuntime.isNotBlank(new StringBuffer("")));
        assertFalse(ContractRuntime.isNotBlank(new StringBuffer("   ")));
    }

    @Test
    void stringBufferNonBlankPasses() {
        assertTrue(ContractRuntime.isNotBlank(new StringBuffer("a")));
    }

    @Test
    void onlyCarriageReturnFails() {
        assertFalse(ContractRuntime.isNotBlank("\r"));
        assertFalse(ContractRuntime.isNotBlank("\r\r\r"));
    }

    @Test
    void onlyNewlineFails() {
        assertFalse(ContractRuntime.isNotBlank("\n"));
        assertFalse(ContractRuntime.isNotBlank("\n\n\n"));
    }

    @Test
    void onlyTabFails() {
        assertFalse(ContractRuntime.isNotBlank("\t"));
        assertFalse(ContractRuntime.isNotBlank("\t\t\t"));
    }
}
