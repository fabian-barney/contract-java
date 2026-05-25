package media.barney.contract.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PatternContractTest {

    @Test
    void nullValuePassesThrough() {
        assertTrue(ContractRuntime.matchesPattern(null, "[0-9]+"));
    }

    @Test
    void simplePatternMatches() {
        assertTrue(ContractRuntime.matchesPattern("123", "[0-9]+"));
        assertTrue(ContractRuntime.matchesPattern("abc", "[a-z]+"));
        assertTrue(ContractRuntime.matchesPattern("ABC", "[A-Z]+"));
    }

    @Test
    void simplePatternDoesNotMatch() {
        assertFalse(ContractRuntime.matchesPattern("abc", "[0-9]+"));
        assertFalse(ContractRuntime.matchesPattern("123", "[a-z]+"));
        assertFalse(ContractRuntime.matchesPattern("123abc", "[0-9]+"));
    }

    @Test
    void emailPatternWorks() {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        assertTrue(ContractRuntime.matchesPattern("user@example.com", emailPattern));
        assertTrue(ContractRuntime.matchesPattern("test.user+tag@domain.co.uk", emailPattern));
        assertFalse(ContractRuntime.matchesPattern("invalid@", emailPattern));
        assertFalse(ContractRuntime.matchesPattern("@example.com", emailPattern));
    }

    @Test
    void phonePatternWorks() {
        String phonePattern = "^\\+?[0-9]{10,15}$";
        assertTrue(ContractRuntime.matchesPattern("1234567890", phonePattern));
        assertTrue(ContractRuntime.matchesPattern("+1234567890", phonePattern));
        assertFalse(ContractRuntime.matchesPattern("123", phonePattern));
        assertFalse(ContractRuntime.matchesPattern("abc", phonePattern));
    }

    @Test
    void uuidPatternWorks() {
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        assertTrue(ContractRuntime.matchesPattern("550e8400-e29b-41d4-a716-446655440000", uuidPattern));
        assertFalse(ContractRuntime.matchesPattern("not-a-uuid", uuidPattern));
    }

    @Test
    void caseSensitivePatternWorks() {
        assertTrue(ContractRuntime.matchesPattern("ABC", "ABC"));
        assertFalse(ContractRuntime.matchesPattern("abc", "ABC"));
        assertFalse(ContractRuntime.matchesPattern("Abc", "ABC"));
    }

    @Test
    void specialCharactersInPatternWork() {
        assertTrue(ContractRuntime.matchesPattern("hello.world", "hello\\.world"));
        assertTrue(ContractRuntime.matchesPattern("price: $100", "price: \\$\\d+"));
        assertFalse(ContractRuntime.matchesPattern("helloXworld", "hello\\.world"));
    }

    @Test
    void anchorPatternsWork() {
        assertTrue(ContractRuntime.matchesPattern("ABC", "^ABC$"));
        assertFalse(ContractRuntime.matchesPattern("ABCD", "^ABC$"));
        assertFalse(ContractRuntime.matchesPattern("DABC", "^ABC$"));
    }

    @Test
    void quantifierPatternsWork() {
        assertTrue(ContractRuntime.matchesPattern("aaa", "a{3}"));
        assertTrue(ContractRuntime.matchesPattern("aa", "a{2,4}"));
        assertTrue(ContractRuntime.matchesPattern("aaa", "a{2,4}"));
        assertTrue(ContractRuntime.matchesPattern("aaaa", "a{2,4}"));
        assertFalse(ContractRuntime.matchesPattern("a", "a{2,4}"));
        assertFalse(ContractRuntime.matchesPattern("aaaaa", "a{2,4}"));
    }

    @Test
    void groupPatternsWork() {
        assertTrue(ContractRuntime.matchesPattern("abcabc", "(abc){2}"));
        assertTrue(ContractRuntime.matchesPattern("abc", "(abc|def)"));
        assertTrue(ContractRuntime.matchesPattern("def", "(abc|def)"));
        assertFalse(ContractRuntime.matchesPattern("ghi", "(abc|def)"));
    }

    @Test
    void emptyStringMatchesEmptyPattern() {
        assertTrue(ContractRuntime.matchesPattern("", ""));
    }

    @Test
    void emptyStringDoesNotMatchNonEmptyPattern() {
        assertFalse(ContractRuntime.matchesPattern("", "[0-9]+"));
    }

    @Test
    void whitespacePatternsWork() {
        assertTrue(ContractRuntime.matchesPattern("   ", "\\s+"));
        assertTrue(ContractRuntime.matchesPattern("\t\n", "\\s+"));
        assertFalse(ContractRuntime.matchesPattern("abc", "\\s+"));
    }

    @Test
    void unicodePatternsWork() {
        assertTrue(ContractRuntime.matchesPattern("你好", "[\\u4e00-\\u9fa5]+"));
        assertTrue(ContractRuntime.matchesPattern("こんにちは", "[\\u3040-\\u309f]+"));
    }

    @Test
    void complexPatternsWork() {
        String datePattern = "^\\d{4}-\\d{2}-\\d{2}$";
        assertTrue(ContractRuntime.matchesPattern("2024-01-15", datePattern));
        assertFalse(ContractRuntime.matchesPattern("2024/01/15", datePattern));
        assertFalse(ContractRuntime.matchesPattern("2024-1-15", datePattern));
    }

    @Test
    void nonCharSequenceTypesFail() {
        assertFalse(ContractRuntime.matchesPattern(123, "[0-9]+"));
        assertFalse(ContractRuntime.matchesPattern(true, "true"));
    }

    @Test
    void stringBuilderWorks() {
        assertTrue(ContractRuntime.matchesPattern(new StringBuilder("123"), "[0-9]+"));
        assertFalse(ContractRuntime.matchesPattern(new StringBuilder("abc"), "[0-9]+"));
    }

    @Test
    void stringBufferWorks() {
        assertTrue(ContractRuntime.matchesPattern(new StringBuffer("123"), "[0-9]+"));
    }

    @Test
    void patternCachingDoesNotAffectResults() {
        for (int i = 0; i < 300; i++) {
            assertTrue(ContractRuntime.matchesPattern("test" + i, "test\\d+"));
        }
    }
}
