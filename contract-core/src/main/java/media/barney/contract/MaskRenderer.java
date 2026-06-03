package media.barney.contract;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Renders confidential values for generated contract violation messages.
 *
 * <p>Implementations are used when {@link Contract.Mask} applies to a
 * parameter or return value. A renderer receives the raw value that would
 * otherwise be rendered in a violation message and must return a representation
 * that is safe to include in exception text.
 *
 * <p>Custom renderers must not leak confidential data unless the application
 * has explicitly approved that representation. For highly sensitive values,
 * prefer fixed text that does not reveal content, length, or structure.
 */
@API(status = API.Status.MAINTAINED)
public interface MaskRenderer {

    /**
     * Renders a confidential value for a violation message.
     *
     * @param value the raw value; may be {@code null}
     * @return the message-safe representation
     */
    String render(@Nullable Object value);
}
