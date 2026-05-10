package media.barney.contract;

/**
 * Core module marker for the contract framework.
 */
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
