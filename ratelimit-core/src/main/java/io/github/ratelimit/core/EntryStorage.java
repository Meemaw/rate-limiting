package io.github.ratelimit.core;

import io.github.ratelimit.core.impl.ConsumptionEntry;

import java.util.List;

public interface EntryStorage {

    /**
     * Function checks and update user rate limit entries in one trip to storage backend.
     *
     * @param identifier to entries
     * @return ConsumptionEntry
     * @throws RateLimiterException
     */
    ConsumptionEntry conformRateLimits(String identifier, int requestWeight) throws RateLimiterException;

    /**
     * Returns entries currently in entry storage. Those are not refilled according to current timestamp.
     *
     * @param identifier to entries
     * @return List<RateLimitEntry> list of rate limit entries
     * @throws RateLimiterException
     */
    List<RateLimitEntry> getCurrentEntries(String identifier) throws RateLimiterException;


    /**
     * Returns refilled rate limit entries from entry storage.
     *
     * @param identifier to entries
     * @return List<RateLimitEntry> list of rate limit entries
     * @throws RateLimiterException
     */
    List<RateLimitEntry> getUpdateEntries(String identifier) throws RateLimiterException;

    /**
     * Stores entries to entry storage.
     *
     * @param identifier to entries
     * @param entries
     * @return
     * @throws RateLimiterException
     */
    void storeEntries(String identifier, List<RateLimitEntry> entries) throws RateLimiterException;

    /**
     * @return get execution timeout for asynchronous operations
     */
    long getExecutionTimeout();

}
