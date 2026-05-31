/**
 * Spring Boot auto-configuration and optional integrations for {@code contract-java}.
 */
module media.barney.contract.spring.boot.starter {
    requires transitive media.barney.contract.core;
    requires transitive org.apiguardian.api;
    requires transitive org.jspecify;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires static jakarta.servlet;
    requires static spring.boot.actuator;
    requires static spring.web;
    requires static spring.webmvc;

    exports media.barney.contract.spring;

    opens media.barney.contract.spring to
            spring.beans,
            spring.boot,
            spring.boot.autoconfigure,
            spring.context,
            spring.core;
}
