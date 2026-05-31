/**
 * Spring Boot auto-configuration and optional integrations for {@code contract-java}.
 */
module media.barney.contract.spring.boot.starter {
    requires transitive media.barney.contract.core;
    requires transitive org.apiguardian.api;
    requires transitive org.jspecify;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.core;
    requires static jakarta.servlet;
    requires static spring.boot.actuator;
    requires static spring.web;
    requires static spring.webmvc;

    exports media.barney.contract.spring;
}
