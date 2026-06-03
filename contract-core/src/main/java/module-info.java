/**
 * Declarative Java contract annotations, generated-code runtime bridge, and annotation processor.
 *
 * <p>The {@code media.barney.contract} package is the application-facing API
 * for declaring semantic contracts. The {@code media.barney.contract.runtime}
 * package is the supported bridge invoked by annotation-processor-generated
 * checks. Packages named {@code internal} are implementation details and are
 * not exported by this module.
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
