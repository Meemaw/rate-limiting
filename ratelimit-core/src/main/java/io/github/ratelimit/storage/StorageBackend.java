package io.github.ratelimit.storage;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.impl.ConsumptionEntry;
import io.github.ratelimit.storage.command.*;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Future;

public interface StorageBackend<K extends Serializable> {

    <T> Future<StorageBackendCommandResult<T>> executeAsync(K key, EntryStorageCommand<T> command);

    Future<Void> storeEntries(K key, List<RateLimitEntry> entries);

    default Future<StorageBackendCommandResult<List<RateLimitEntry>>> getUpdateRateLimitEntries(K key) {
        return executeAsync(key, new GetUpdateEntries());
    }

    default Future<StorageBackendCommandResult<List<RateLimitEntry>>> getRateLimitEntries(K key) {
        return executeAsync(key, new GetEntries());
    }

    default Future<StorageBackendCommandResult<ConsumptionEntry>> conformsRateLimitsWithStatus(K key, int requestWeight) {
        return executeAsync(key, new ConformRateLimits(requestWeight));
    }

}
