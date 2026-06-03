package media.barney.contract;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Conservative mask renderer that never reveals anything about the original value.
 *
 * <p>This renderer always returns {@code [MASKED]}, including when the raw
 * value is {@code null}. It intentionally avoids exposing content, length,
 * type-specific formatting, or structural hints about confidential values.
 */
@API(status = API.Status.MAINTAINED)
public final class DefaultMaskRenderer implements MaskRenderer {

    private static final String MASKED_VALUE = "[MASKED]";

    /**
     * Returns the fixed masked representation for any raw value.
     *
     * @param value the raw value; ignored by this implementation
     * @return {@code [MASKED]}
     */
    @Override
    public String render(@Nullable Object value) {
        return MASKED_VALUE;
    }
}
