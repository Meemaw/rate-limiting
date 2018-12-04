package io.github.ratelimit.core;

public final class RateLimitExceptions {

    private RateLimitExceptions() {
    }

    public static IllegalArgumentException unsupportedTechnique() {
        String msg = "Rate limiting technique is not supported";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveNumber(long value) {
        String msg = String.format("Positive number expected got %d", value);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException tokenConsumptionsExcedsPolicy(long tokenConsumption, long capacity) {
        String msg = String.format("Policy capacity is %d but got tokenCunsumption of %d", capacity, tokenConsumption);
        return new IllegalArgumentException(msg);
    }

}
