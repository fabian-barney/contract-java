package media.barney.contract.runtime.internal;

import media.barney.contract.MaskRenderer;

public record ContractEvaluation(boolean valid, ContractRule rule, Class<? extends MaskRenderer> maskRenderer) {

    static ContractEvaluation valid(Class<? extends MaskRenderer> maskRenderer) {
        return new ContractEvaluation(true, ContractRule.none(), maskRenderer);
    }

    static ContractEvaluation invalid(ContractRule rule, Class<? extends MaskRenderer> maskRenderer) {
        return new ContractEvaluation(false, rule, maskRenderer);
    }
}
