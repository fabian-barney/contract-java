package media.barney.contract.runtime.internal;

public record ContractRule(String description, boolean customDescription) {

    private static final ContractRule NONE = new ContractRule("", false);

    static ContractRule none() {
        return NONE;
    }
}
