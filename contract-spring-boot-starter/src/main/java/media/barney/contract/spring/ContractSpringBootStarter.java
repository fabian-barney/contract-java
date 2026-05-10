package media.barney.contract.spring;

/**
 * Starter module marker for dependency-management integration.
 */
public final class ContractSpringBootStarter {

    private static final String ARTIFACT_ID = "contract-spring-boot-starter";

    private ContractSpringBootStarter() {
        throw new AssertionError("No instances.");
    }

    public static String artifactId() {
        return ARTIFACT_ID;
    }
}
