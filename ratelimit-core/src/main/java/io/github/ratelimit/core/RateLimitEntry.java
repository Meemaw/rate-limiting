package io.github.ratelimit.core;

import io.github.ratelimit.core.impl.ConsumptionEntry;

import java.io.Externalizable;
import java.time.Instant;

public interface RateLimitEntry extends Externalizable {

    /**
     * Updates RateLimitRecord according to RefillPolicy and passed timestamp.
     *
     * @param timestamp current timestamp
     * @return updated entry
     */
    RateLimitEntry update(Instant timestamp);

    /**
     * @param numTokens
     * @return Rate limit record
     */
    RateLimitEntry consumeInPlace(long numTokens);

    /**
     * Updates RateLimitRecord according to RefillPolicy and passed timestamp.
     * Mutates the entry
     *
     * @param timestamp current timestamp
     * @return updated entry
     */
    RateLimitEntry updateInPlace(Instant timestamp);

    /**
     * Updates RateLimitRecord according to RefillPolicy and current timestamp.
     * Mutates the entry!
     *
     * @return updated entry
     */
    default RateLimitEntry update() {
        return update(Instant.now());
    }

    /**
     * @param numTokens to consume
     * @return ConsumptionEntry with numTokens subtracted and mutated
     */
    ConsumptionEntry tryConsumeAndReturnRemaining(long numTokens);

    /**
     * @param numTokens to consume
     * @return ConsumptionEntry with numTokens subtracted but not mutated
     */
    ConsumptionEntry canConsumeAndReturnRemaining(long numTokens, Instant ts);

    /**
     * @return number of tokens in entry
     */
    default long getTokenCount() {
        return getRateLimitRecord().getTokenCount();
    }

    /**
     * @param consumptionCount number of tokens to consume
     * @return long time until next successful consumption in nano seconds
     */
    long getNanosToConsumption(long consumptionCount);

    /**
     * @return long time until next refill in nano seconds
     */
    default long getNanosToWaitForRefill() {
        return getNanosToConsumption(1L);
    }

    /**
     * Mutates the entry for consumption! Use canConsume for pure check
     *
     * @param numTokens number of tokens to consume
     * @param ts
     * @return boolean whether consumption was successful
     */
    boolean tryConsume(long numTokens, Instant ts);

    /**
     * Mutates the entry for consumption! Use canConsume for pure check. Same as
     * calling tryConsume(numTokens, Instant.now());
     *
     * @param numTokens to consume
     * @return boolean whether consumption was successful
     */
    default boolean tryConsume(long numTokens) {
        return tryConsume(numTokens, Instant.now());
    }

    /**
     * @param numTokens to consume
     * @return boolean whether consumption was successful
     */
    default boolean canConsume(long numTokens) {
        return getRateLimitRecord().canConsume(numTokens);
    }

    /**
     * @return long number of missing tokens
     */
    default long getMissingTokens() {
        return getRefillPolicy().getCapacity() - getRateLimitRecord().getTokenCount();
    }

    /**
     * @return Refill policy
     */
    RefillPolicy getRefillPolicy();

    /**
     * @return Rate limit record
     */
    RateLimitRecord getRateLimitRecord();

}

