package media.barney.contract.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import media.barney.contract.ContractCore;
import org.junit.jupiter.api.Test;

class ContractSpringBootStarterTest {

    @Test
    void exposesContractCoreDependency() {
        assertEquals("contract-core", ContractCore.artifactId());
    }

    @Test
    void returnsArtifactId() {
        assertEquals("contract-spring-boot-starter", ContractSpringBootStarter.artifactId());
    }
}
