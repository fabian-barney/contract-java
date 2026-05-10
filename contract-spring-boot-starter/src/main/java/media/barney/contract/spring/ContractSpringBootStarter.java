package media.barney.contract.spring;

/**
 * Starter module marker for dependency-management integration.
 */
public final class ContractSpringBootStarter {

    private static final String ARTIFACT_ID = "contract-spring-boot-starter";

    private ContractSpringBootStarter() {
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
