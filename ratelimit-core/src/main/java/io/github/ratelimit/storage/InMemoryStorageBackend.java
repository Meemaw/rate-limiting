package io.github.ratelimit.storage;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.storage.command.EntryStorageCommand;
import io.github.ratelimit.storage.command.StorageBackendCommandResult;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class InMemoryStorageBackend<K extends Serializable> implements StorageBackend<K> {

    private final Map<K, List<RateLimitEntry>> cache;

    public InMemoryStorageBackend() {
        this.cache = new HashMap<>();
    }

    public InMemoryStorageBackend(Map<K, List<RateLimitEntry>> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    public <T> StorageBackendCommandResult<T> execute(K key, EntryStorageCommand<T> command) {
        List<RateLimitEntry> entries = cache.get(key);
        if (entries == null) {
            return StorageBackendCommandResult.entriesNotFound();
        }

        T result = command.execute(entries, Instant.now());
        return result != null ? StorageBackendCommandResult.success(result)
                : StorageBackendCommandResult.entriesNotFound();
    }

    @Override
    public <T> CompletableFuture<StorageBackendCommandResult<T>> executeAsync(K key, EntryStorageCommand<T> command) {
        return CompletableFuture.completedFuture(execute(key, command));
    }

    @Override
    public Future<Void> storeEntries(K key, List<RateLimitEntry> entries) {
        cache.put(key, entries);
        return CompletableFuture.completedFuture(null);
    }

}
