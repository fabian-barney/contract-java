package media.barney.contract;

/**
 * Renders confidential values for generated contract violation messages.
 */
public interface MaskRenderer {

    String render(Object value);
}
