package media.barney.contract.runtime;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import media.barney.contract.DefaultMaskRenderer;
import media.barney.contract.MaskRenderer;

final class ValueRenderer {

    private static final MaskRenderer DEFAULT_MASK_RENDERER = new DefaultMaskRenderer();
    private static final Map<Character, String> ESCAPES = Map.of(
            '\\', "\\\\",
            '"', "\\\"",
            '\n', "\\n",
            '\r', "\\r",
            '\t', "\\t");

    private ValueRenderer() {
    }

    static String render(Object value, Class<? extends MaskRenderer> maskRenderer) {
        if (maskRenderer != null) {
            return renderMasked(value, maskRenderer);
        }

        return renderUnmasked(value);
    }

    private static String renderMasked(Object value, Class<? extends MaskRenderer> maskRenderer) {
        try {
            return newRenderer(maskRenderer).render(value);
        } catch (Throwable throwable) {
            rethrowFatal(throwable);
            return DEFAULT_MASK_RENDERER.render(value);
        }
    }

    private static void rethrowFatal(Throwable throwable) {
        if (throwable instanceof VirtualMachineError error) {
            throw error;
        }
    }

    private static MaskRenderer newRenderer(Class<? extends MaskRenderer> maskRenderer) {
        try {
            return maskRenderer.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            return DEFAULT_MASK_RENDERER;
        }
    }

    private static String renderUnmasked(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence sequence) {
            return '"' + escape(sequence) + '"';
        }
        if (value instanceof Character character) {
            return "'" + escape(character.toString()) + "'";
        }
        if (value.getClass().isArray()) {
            return renderArray(value);
        }

        return String.valueOf(value);
    }

    private static String renderArray(Object array) {
        if (array instanceof Object[] objects) {
            return Arrays.deepToString(objects);
        }

        int length = Array.getLength(array);
        Object[] boxed = new Object[length];
        for (int index = 0; index < length; index++) {
            boxed[index] = Array.get(array, index);
        }

        return Arrays.toString(boxed);
    }

    private static String escape(CharSequence sequence) {
        StringBuilder escaped = new StringBuilder(sequence.length());
        for (int index = 0; index < sequence.length(); index++) {
            escaped.append(escape(sequence.charAt(index)));
        }

        return escaped.toString();
    }

    private static String escape(char character) {
        String escaped = ESCAPES.get(character);
        return escaped == null ? Character.toString(character) : escaped;
    }
}
