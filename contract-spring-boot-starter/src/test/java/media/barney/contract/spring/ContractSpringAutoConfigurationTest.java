package media.barney.contract.spring;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ContractSpringAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ContractSpringAutoConfiguration.class));

    @Test
    void exposesDefaultPropertiesAndActuatorInfoContributor() {
        contextRunner.run(context -> {
            ContractSpringProperties properties = context.getBean(ContractSpringProperties.class);
            assertTrue(properties.getActuatorInfo().isEnabled());

            InfoContributor contributor = context.getBean(InfoContributor.class);
            Info.Builder builder = new Info.Builder();
            contributor.contribute(builder);

            assertTrue(builder.build().getDetails().containsKey("contract-java"));
        });
    }

    @Test
    void disablesActuatorInfoContributorWhenConfigured() {
        contextRunner
                .withPropertyValues("contract.spring.actuator-info.enabled=false")
                .run(context -> {
                    assertTrue(context.getBeansOfType(InfoContributor.class).isEmpty());
                });
    }

    @Test
    void backsOffWhenActuatorClassesAreAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(InfoContributor.class))
                .run(context -> {
                    assertNotNull(context.getBean(ContractSpringProperties.class));
                    assertTrue(context.getBeansOfType(InfoContributor.class).isEmpty());
                });
    }
}
