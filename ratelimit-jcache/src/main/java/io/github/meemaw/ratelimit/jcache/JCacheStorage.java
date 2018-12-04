package io.github.meemaw.ratelimit.jcache;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.storage.RateLimitEntryProcessor;
import io.github.ratelimit.storage.StorageBackend;
import io.github.ratelimit.storage.command.EntryStorageCommand;
import io.github.ratelimit.storage.command.StorageBackendCommandResult;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class JCacheStorage<K extends Serializable> implements StorageBackend<K> {

    private final Cache<K, List<RateLimitEntry>> cache;

    public JCacheStorage(Cache<K, List<RateLimitEntry>> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    private <T> StorageBackendCommandResult<T> execute(K key, EntryStorageCommand<T> command) {
        RateLimitEntryProcessor<K, T> entryProcessor = new RateLimitEntryProcessor<>(command);
        return cache.invoke(key, entryProcessor);
    }


    @Override
    public <T> Future<StorageBackendCommandResult<T>> executeAsync(K key, EntryStorageCommand<T> command) {
        return CompletableFuture.completedFuture(execute(key, command));
    }

    @Override
    public Future<Void> storeEntries(K key, List<RateLimitEntry> entries) {
        cache.put(key, entries);
        return CompletableFuture.completedFuture(null);

    }
}
