package io.github.meemaw.ratelimit.hazelcast;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryProcessor;
import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.storage.RateLimitEntryProcessor;
import io.github.ratelimit.storage.StorageBackend;
import io.github.ratelimit.storage.command.EntryStorageCommand;
import io.github.ratelimit.storage.command.StorageBackendCommandResult;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class HazelcastStorage implements StorageBackend<String> {

    private final IMap<String, List<RateLimitEntry>> rateLimitCache;

    private final static String RATELIMIT_IDENTIFIER = System.getProperty("ratelimit.map.users" +
            ".limits", "ratelimit.map.users.limits");

    public HazelcastStorage(HazelcastInstance hzInstance) {
        this.rateLimitCache = Objects.requireNonNull(hzInstance).getMap(RATELIMIT_IDENTIFIER);
    }

    @Override
    public <T> CompletableFuture<StorageBackendCommandResult<T>> executeAsync(String key, EntryStorageCommand<T> command) {
        RateLimitEntryProcessor<String, T> entryProcessor = new RateLimitEntryProcessor<>(command);
        return invokeAsync(key, entryProcessor);
    }

    @Override
    public Future<Void> storeEntries(final String key, final List<RateLimitEntry> entries) {
        return rateLimitCache.setAsync(key, entries);
    }

    private <T> EntryProcessor<String, ?> adoptEntryProcessor(
            RateLimitEntryProcessor<String, T>
                    entryProcessor) {
        return new HazelcastRateLimitEntryProcessor<>(entryProcessor);
    }

    private <T> CompletableFuture<StorageBackendCommandResult<T>> invokeAsync(String key, RateLimitEntryProcessor<String, T>
            entryProcessor) {
        CompletableFuture<StorageBackendCommandResult<T>> future = new CompletableFuture<>();
        rateLimitCache.submitToKey(key, adoptEntryProcessor(entryProcessor), new
                ExecutionCallback<StorageBackendCommandResult<T>>() {
                    @Override
                    public void onResponse(StorageBackendCommandResult<T> response) {
                        future.complete(response);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }

}
