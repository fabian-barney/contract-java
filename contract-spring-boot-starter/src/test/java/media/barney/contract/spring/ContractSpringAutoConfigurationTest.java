package media.barney.contract.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import media.barney.contract.DefaultMaskRenderer;
import media.barney.contract.runtime.ContractRuntime;
import media.barney.contract.runtime.RuntimeContract;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class ContractSpringAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ContractSpringAutoConfiguration.class));

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ContractSpringAutoConfiguration.class));

    @Test
    void exposesDefaultPropertiesAndActuatorInfoContributor() {
        contextRunner.run(context -> {
            ContractSpringProperties properties = context.getBean(ContractSpringProperties.class);
            assertFalse(properties.getWebExceptionHandler().isEnabled());
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

    @Test
    void doesNotRegisterWebExceptionHandlerByDefault() {
        webContextRunner.run(context -> {
            assertTrue(context.getBeansOfType(HandlerExceptionResolver.class).isEmpty());
        });
    }

    @Test
    void registersWebExceptionHandlerWhenEnabled() {
        webContextRunner
                .withPropertyValues("contract.spring.web-exception-handler.enabled=true")
                .run(context -> {
                    assertEquals(
                            1,
                            context.getBeansOfType(HandlerExceptionResolver.class)
                                    .size());
                });
    }

    @Test
    void mapsGeneratedPreconditionsTo500() throws Exception {
        webContextRunner
                .withPropertyValues("contract.spring.web-exception-handler.enabled=true")
                .run(context -> {
                    HandlerExceptionResolver resolver = context.getBean(HandlerExceptionResolver.class);
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    ModelAndView modelAndView = resolver.resolveException(
                            new MockHttpServletRequest(), response, null, generatedPreconditionViolation());

                    assertNotNull(modelAndView);
                    assertEquals(500, response.getStatus());
                    assertEquals("", response.getContentAsString());
                });
    }

    @Test
    void mapsGeneratedPostconditionsTo500() throws Exception {
        webContextRunner
                .withPropertyValues("contract.spring.web-exception-handler.enabled=true")
                .run(context -> {
                    HandlerExceptionResolver resolver = context.getBean(HandlerExceptionResolver.class);
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    ModelAndView modelAndView = resolver.resolveException(
                            new MockHttpServletRequest(), response, null, generatedPostconditionViolation());

                    assertNotNull(modelAndView);
                    assertEquals(500, response.getStatus());
                    assertEquals("", response.getContentAsString());
                });
    }

    @Test
    void ignoresNonContractIllegalArgumentExceptions() {
        webContextRunner
                .withPropertyValues("contract.spring.web-exception-handler.enabled=true")
                .run(context -> {
                    HandlerExceptionResolver resolver = context.getBean(HandlerExceptionResolver.class);
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    ModelAndView modelAndView = resolver.resolveException(
                            new MockHttpServletRequest(), response, null, new IllegalArgumentException("bad request"));

                    assertNull(modelAndView);
                    assertEquals(200, response.getStatus());
                });
    }

    @Test
    void ignoresNonContractIllegalStateExceptions() {
        webContextRunner
                .withPropertyValues("contract.spring.web-exception-handler.enabled=true")
                .run(context -> {
                    HandlerExceptionResolver resolver = context.getBean(HandlerExceptionResolver.class);
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    ModelAndView modelAndView = resolver.resolveException(
                            new MockHttpServletRequest(), response, null, new IllegalStateException("bad state"));

                    assertNull(modelAndView);
                    assertEquals(200, response.getStatus());
                });
    }

    @Test
    void ignoresContractExceptionsWhenResponseIsCommitted() {
        webContextRunner
                .withPropertyValues("contract.spring.web-exception-handler.enabled=true")
                .run(context -> {
                    HandlerExceptionResolver resolver = context.getBean(HandlerExceptionResolver.class);
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    response.setCommitted(true);

                    ModelAndView modelAndView = resolver.resolveException(
                            new MockHttpServletRequest(), response, null, generatedPostconditionViolation());

                    assertNull(modelAndView);
                });
    }

    private static IllegalArgumentException generatedPreconditionViolation() {
        try {
            ContractRuntime.requireParameterValue(
                    "secret",
                    "example.AccountService.login",
                    "password",
                    RuntimeContract.PATTERN,
                    "must match the required pattern",
                    false,
                    DefaultMaskRenderer.class,
                    0L,
                    0L,
                    true,
                    true,
                    0,
                    0,
                    "[0-9]+");
        } catch (IllegalArgumentException exception) {
            return exception;
        }

        throw new AssertionError("Expected generated precondition violation.");
    }

    private static IllegalStateException generatedPostconditionViolation() {
        try {
            ContractRuntime.requireReturnValue(
                    "bad",
                    "example.TokenService.issue",
                    RuntimeContract.PATTERN,
                    "must match the required pattern",
                    false,
                    DefaultMaskRenderer.class,
                    0L,
                    0L,
                    true,
                    true,
                    0,
                    0,
                    "USR-[0-9]+");
        } catch (IllegalStateException exception) {
            return exception;
        }

        throw new AssertionError("Expected generated postcondition violation.");
    }
}
