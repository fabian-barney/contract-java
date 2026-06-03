package media.barney.contract.spring;

import org.apiguardian.api.API;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for optional Spring Boot integrations.
 *
 * <p>Properties use the {@code contract.spring} prefix. All integrations are
 * conditional: setting these properties only affects beans whose required
 * Spring Boot, actuator, or servlet classes are available.
 */
@API(status = API.Status.EXPERIMENTAL)
@ConfigurationProperties(prefix = "contract.spring")
public class ContractSpringProperties {

    private final WebExceptionHandlerSettings webExceptionHandler = new WebExceptionHandlerSettings();

    private final ActuatorInfoSettings actuatorInfo = new ActuatorInfoSettings();

    /**
     * Returns settings for the opt-in servlet exception resolver.
     *
     * <p>The resolver is disabled by default. When enabled in a servlet web
     * application, it maps generated contract violations to bodyless HTTP 500
     * responses.
     *
     * @return mutable web exception handler settings
     */
    public WebExceptionHandlerSettings getWebExceptionHandler() {
        return webExceptionHandler;
    }

    /**
     * Returns settings for the optional actuator information contributor.
     *
     * <p>The contributor is enabled by default when actuator classes are on the
     * classpath. It reports that the contract Spring Boot starter is active.
     *
     * @return mutable actuator info settings
     */
    public ActuatorInfoSettings getActuatorInfo() {
        return actuatorInfo;
    }

    /**
     * Settings for {@code contract.spring.web-exception-handler}.
     *
     * <p>This integration is intentionally opt-in. It handles only generated
     * contract violations and maps them to server errors because contract
     * violations indicate programming mistakes, not recoverable request
     * validation failures.
     */
    @API(status = API.Status.EXPERIMENTAL)
    public static final class WebExceptionHandlerSettings {

        private boolean enabled;

        /**
         * Returns whether the servlet exception resolver is enabled.
         *
         * @return {@code true} when the resolver should be registered
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the servlet exception resolver is enabled.
         *
         * @param enabled {@code true} to register the resolver when servlet MVC
         *     classes are available
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Settings for {@code contract.spring.actuator-info}.
     *
     * <p>This integration is enabled by default but remains conditional on
     * Spring Boot actuator being present.
     */
    @API(status = API.Status.EXPERIMENTAL)
    public static final class ActuatorInfoSettings {

        private boolean enabled = true;

        /**
         * Returns whether the actuator info contributor is enabled.
         *
         * @return {@code true} when the contributor should be registered
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the actuator info contributor is enabled.
         *
         * @param enabled {@code true} to register the contributor when actuator
         *     classes are available
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
