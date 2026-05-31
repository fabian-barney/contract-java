package media.barney.contract.spring;

import java.util.Map;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for optional {@code contract-java} integrations.
 */
@AutoConfiguration
@EnableConfigurationProperties(ContractSpringProperties.class)
public class ContractSpringAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(InfoContributor.class)
    static class ActuatorInfoConfiguration {

        @Bean
        @ConditionalOnProperty(
                prefix = "contract.spring.actuator-info",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true)
        InfoContributor contractSpringInfoContributor() {
            return builder -> builder.withDetail(
                    "contract-java",
                    Map.of(
                            "module",
                            "contract-spring-boot-starter",
                            "integration",
                            "spring-boot-auto-configuration",
                            "enforcement",
                            "runtime"));
        }
    }
}
