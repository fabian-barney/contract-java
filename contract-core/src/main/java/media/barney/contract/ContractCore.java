package media.barney.contract;

import org.apiguardian.api.API;

/**
 * Core module marker for the contract framework.
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
