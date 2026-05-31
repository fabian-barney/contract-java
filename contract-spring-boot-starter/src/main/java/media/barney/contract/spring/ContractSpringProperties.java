package media.barney.contract.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for optional Spring Boot integrations.
 */
@ConfigurationProperties(prefix = "contract.spring")
public class ContractSpringProperties {

    private final WebExceptionHandlerSettings webExceptionHandler = new WebExceptionHandlerSettings();

    private final ActuatorInfoSettings actuatorInfo = new ActuatorInfoSettings();

    public WebExceptionHandlerSettings getWebExceptionHandler() {
        return webExceptionHandler;
    }

    public ActuatorInfoSettings getActuatorInfo() {
        return actuatorInfo;
    }

    public static final class WebExceptionHandlerSettings {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static final class ActuatorInfoSettings {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
