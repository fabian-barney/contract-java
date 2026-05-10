package media.barney.contract.runtime;

record ContractRule(String description, boolean customDescription) {

    private static final ContractRule NONE = new ContractRule("", false);

    static ContractRule none() {
        return NONE;
    }
}
