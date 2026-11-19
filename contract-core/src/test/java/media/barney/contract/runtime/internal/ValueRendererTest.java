package media.barney.contract.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ValueRendererTest {

    @Test
    void quotesEscapesAndFormatsArrays() {
        assertEquals("\"a\\n\\\"\\\\\\t\\r\"", ValueRenderer.render("a\n\"\\\t\r", null));
        assertEquals("'\\n'", ValueRenderer.render('\n', null));
        assertEquals("[1, 2]", ValueRenderer.render(new int[] {1, 2}, null));
        assertEquals("[a, [b]]", ValueRenderer.render(new Object[] {"a", new String[] {"b"}}, null));
    }
}
