package media.barney.contract.runtime.internal;

public record ContractArguments(
        long min, long max, boolean minInclusive, boolean maxInclusive, int sizeMin, int sizeMax, String regexp) {}
