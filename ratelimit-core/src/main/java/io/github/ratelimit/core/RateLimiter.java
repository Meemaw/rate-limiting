package io.github.ratelimit.core;

import io.github.ratelimit.core.impl.ConsumptionEntry;

import java.util.Map;

public interface RateLimiter {

    /**
     * Same as calling conformsRateLimits(identifier, 1);
     *
     * @param identifier
     * @return boolean
     * @throws RateLimiterException
     */
    default boolean conformsRateLimits(String identifier) throws RateLimiterException {
        return conformsRateLimits(identifier, 1);
    }

    /**
     * Same as calling conformsRateLimitsWithConsumption(identifier, 1);
     *
     * @param identifier
     * @return ConsumptionEntry
     * @throws RateLimiterException
     */
    default ConsumptionEntry conformRateLimitsWithConsumption(String identifier)
            throws RateLimiterException {
        return conformRateLimitsWithConsumption(identifier, 1);
    }

    /**
     * @param identifier
     * @param requestWeight
     * @return boolean whether identity passes rate limits
     * @throws RateLimiterException
     */
    default boolean conformsRateLimits(String identifier, int requestWeight) throws RateLimiterException {
        return conformRateLimitsWithConsumption(identifier, requestWeight).doesConform();
    }

    /**
     * Returns Optional.empty() if no identity has no refillPolicies.
     *
     * @param identifier
     * @param requestWeight
     * @return ConsumptionEntry
     * @throws RateLimiterException
     */
    ConsumptionEntry conformRateLimitsWithConsumption(String identifier, int requestWeight)
            throws RateLimiterException;

    /**
     * Get missing token counts for RefillPolicies. This method is
     * RateLimitAlgorithm specific and can returns different values.
     *
     * @param identifier
     * @return Map<String, Long>
     * @throws RateLimiterException
     */
    Map<String, Long> getMissingTokenCounts(String identifier) throws RateLimiterException;

    /**
     * Get remaining token count for RefillPolicies. This method is
     * RateLimitAlgorithm specific and can return different values.
     *
     * @param identifier
     * @return Map<String, Long>
     * @throws RateLimiterException
     */
    Map<String, Long> getTokenCounts(String identifier) throws RateLimiterException;

    /**
     * Get underlying EntryStorage of RateLimiter.
     *
     * @return EntryStorage
     */
    EntryStorage getStorage();

}
