package media.barney.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class ContractCoreTest {

    @Test
    void returnsArtifactId() {
        assertEquals("contract-core", ContractCore.artifactId());
    }

    @Test
    void rejectsInstantiation() throws Exception {
        Constructor<ContractCore> constructor = ContractCore.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                constructor::newInstance);

        assertEquals(AssertionError.class, exception.getCause().getClass());
    }
}
