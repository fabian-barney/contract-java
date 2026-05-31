package media.barney.contract.spring;

import java.util.Map;
import org.apiguardian.api.API;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring Boot auto-configuration for optional {@code contract-java} integrations.
 */
@API(status = API.Status.EXPERIMENTAL)
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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(HandlerExceptionResolver.class)
    static class WebExceptionHandlerConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = "contract.spring.web-exception-handler", name = "enabled", havingValue = "true")
        HandlerExceptionResolver contractSpringWebExceptionHandler() {
            return new ContractViolationHandlerExceptionResolver();
        }
    }
}
