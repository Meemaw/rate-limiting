package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.EntryStorage;
import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimiter;
import io.github.ratelimit.core.RateLimiterException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RateLimiterImpl implements RateLimiter {

    private final EntryStorage entryStorage;

    public RateLimiterImpl(EntryStorage entryStorage) {
        this.entryStorage = entryStorage;
    }

    public static RateLimiterImpl withStorage(EntryStorage storage) {
        return new RateLimiterImpl(storage);
    }

    @Override
    public ConsumptionEntry conformRateLimitsWithConsumption(String identifier, int requestWeight)
            throws RateLimiterException {
        return entryStorage.conformRateLimits(identifier, requestWeight);
    }

    @Override
    public Map<String, Long> getMissingTokenCounts(String identifier) throws RateLimiterException {
        return getStats(identifier, (RateLimitEntry e) -> e.getMissingTokens());
    }

    @Override
    public Map<String, Long> getTokenCounts(String identifier) throws RateLimiterException {
        return getStats(identifier, (RateLimitEntry e) -> e.getTokenCount());
    }

    @Override
    public EntryStorage getStorage() {
        return entryStorage;
    }

    private Map<String, Long> getStats(String identifier, Function<RateLimitEntry, Long> statFunction)
            throws RateLimiterException {
        List<RateLimitEntry> updated = entryStorage.getUpdateEntries(identifier);

        Map<String, Long> statMap = new HashMap<>(4);
        for (RateLimitEntry entry : updated) {
            statMap.put(entry.getRefillPolicy().getSamplingPeriod().toString(), statFunction.apply(entry));
        }
        return statMap;
    }

}
