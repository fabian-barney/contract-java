package media.barney.contract.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.TestEngine;
import org.springframework.boot.SpringBootVersion;

class SelectedDependencyVersionsTest {

    @Test
    void runsAgainstSelectedSpringBootVersion() {
        String expected = System.getProperty("expected.spring.boot.version");
        assertNotNull(expected);
        assertEquals(expected, SpringBootVersion.getVersion());
    }

    @Test
    void alignsJUnitApiEngineAndPlatformVersions() {
        String expected = System.getProperty("expected.junit.version");
        assertNotNull(expected);
        for (Class<?> component :
                List.of(Test.class, JupiterTestEngine.class, TestEngine.class, JUnitException.class)) {
            assertEquals(expected, component.getPackage().getImplementationVersion(), component.getName());
        }
    }
}
