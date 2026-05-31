/**
 * Declarative Java contract annotations, generated-code runtime bridge, and annotation processor.
 */
module media.barney.contract.core {
    requires transitive org.apiguardian.api;
    requires transitive org.jspecify;
    requires static java.compiler;
    requires static jdk.compiler;

    exports media.barney.contract;
    exports media.barney.contract.runtime;

    provides javax.annotation.processing.Processor with
            media.barney.contract.processor.internal.ContractProcessorProvider;
}
