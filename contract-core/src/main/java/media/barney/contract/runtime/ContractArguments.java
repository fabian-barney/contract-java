package media.barney.contract.runtime;

record ContractArguments(
        long min, long max, boolean minInclusive, boolean maxInclusive, int sizeMin, int sizeMax, String regexp) {}
