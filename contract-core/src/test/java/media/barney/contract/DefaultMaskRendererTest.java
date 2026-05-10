package media.barney.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DefaultMaskRendererTest {

    @Test
    void masksWithoutRevealingValue() {
        MaskRenderer renderer = new DefaultMaskRenderer();

        assertEquals("[MASKED]", renderer.render("secret-value"));
        assertEquals("[MASKED]", renderer.render(null));
    }
}
