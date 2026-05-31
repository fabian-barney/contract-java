package media.barney.contract;

import org.jspecify.annotations.Nullable;

/**
 * Renders confidential values for generated contract violation messages.
 */
public interface MaskRenderer {

    /**
     * Renders a confidential value for a violation message.
     *
     * @param value the raw value
     * @return the masked representation
     */
    String render(@Nullable Object value);
}
