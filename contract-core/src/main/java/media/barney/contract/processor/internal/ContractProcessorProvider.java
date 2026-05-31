package media.barney.contract.processor.internal;

import javax.annotation.processing.Processor;

/**
 * JPMS service provider bridge for the contract annotation processor.
 */
public final class ContractProcessorProvider {

    private ContractProcessorProvider() {
        throw new AssertionError("No instances.");
    }

    public static Processor provider() {
        return new ContractProcessor();
    }
}
