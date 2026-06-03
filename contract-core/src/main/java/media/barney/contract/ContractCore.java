package media.barney.contract;

import org.apiguardian.api.API;

/**
 * Core module marker for the contract framework.
 *
 * <p>This class exposes a small stable identifier for applications and
 * integrations that need to report the active {@code contract-core} artifact.
 * Contract declaration and enforcement APIs are provided by
 * {@link Contract}, {@link MaskRenderer}, and the generated-code runtime bridge
 * in {@code media.barney.contract.runtime}.
 */
@API(status = API.Status.MAINTAINED)
public final class ContractCore {

    private static final String ARTIFACT_ID = "contract-core";

    private ContractCore() {
        throw new AssertionError("No instances.");
    }

    /**
     * Returns the Maven artifact identifier for this module.
     *
     * @return the artifact identifier
     */
    public static String artifactId() {
        return ARTIFACT_ID;
    }
}
