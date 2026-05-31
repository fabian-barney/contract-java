package media.barney.contract;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Conservative mask renderer that never reveals anything about the original value.
 */
@API(status = API.Status.MAINTAINED)
public final class DefaultMaskRenderer implements MaskRenderer {

    private static final String MASKED_VALUE = "[MASKED]";

    @Override
    public String render(@Nullable Object value) {
        return MASKED_VALUE;
    }
}
