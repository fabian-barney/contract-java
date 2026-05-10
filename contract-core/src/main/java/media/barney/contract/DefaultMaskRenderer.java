package media.barney.contract;

/**
 * Conservative mask renderer that never reveals anything about the original value.
 */
public final class DefaultMaskRenderer implements MaskRenderer {

    private static final String MASKED_VALUE = "[MASKED]";

    @Override
    public String render(Object value) {
        return MASKED_VALUE;
    }
}
