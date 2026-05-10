package media.barney.contract.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    @Test
    void rejectsInstantiation() throws Exception {
        Constructor<ContractSpringBootStarter> constructor = ContractSpringBootStarter.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                constructor::newInstance);

        assertEquals(AssertionError.class, exception.getCause().getClass());
    }
}
