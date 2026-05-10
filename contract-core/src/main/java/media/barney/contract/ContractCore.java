package media.barney.contract;

/**
 * Core module marker for the contract framework.
 */
public final class ContractCore {

    private static final String ARTIFACT_ID = "contract-core";

    private ContractCore() {
        throw new AssertionError("No instances.");
    }

    public static String artifactId() {
        return ARTIFACT_ID;
    }
}
