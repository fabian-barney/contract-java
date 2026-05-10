package media.barney.contract;

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
    String render(Object value);
}
