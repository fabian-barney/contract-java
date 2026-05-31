package media.barney.contract;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Renders confidential values for generated contract violation messages.
 */
@API(status = API.Status.MAINTAINED)
public interface MaskRenderer {

    /**
     * Renders a confidential value for a violation message.
     *
     * @param value the raw value
     * @return the masked representation
     */
    String render(@Nullable Object value);
}
