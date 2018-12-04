package io.github.ratelimit.core;


import java.time.Instant;

public interface RateLimitRecord {

    /**
     * @param refillPolicy
     * @param timestamp
     * @return RateLimitRecord updated with new tokens according to refillPolicy
     */
    RateLimitRecord updateWith(RefillPolicy refillPolicy, Instant timestamp);

    /**
     * @return long number of tokens in record
     */
    long getTokenCount();

    /**
     * @param tokens to cunsume
     * @return RateLimitRecord with updated tokenCoun
     */
    RateLimitRecord consume(long tokens);

    /**
     * @param tokens to consume
     * @return boolean whether consumption is possible
     */
    boolean canConsume(long tokens);

    /**
     * @param refillPolicy
     * @param tokens       to consume
     * @return long time to consumption in nano seconds
     */
    default long getNanosToConsumption(RefillPolicy refillPolicy, long tokens) {
        return getNanosToConsumption(refillPolicy, tokens, false);
    }

    /**
     * @param refillPolicy
     * @param tokens              to consume
     * @param throwOnPolicyExceds boolean whether to throw on impossible consumption
     * @return long time to consumption in nano seconds
     * @throws IllegalArgumentException
     */
    long getNanosToConsumption(RefillPolicy refillPolicy, long tokens, boolean throwOnPolicyExceds) throws IllegalArgumentException;

}
