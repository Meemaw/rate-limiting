package io.github.ratelimit.storage;

import io.github.ratelimit.core.EntryStorage;
import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimiterException;
import io.github.ratelimit.core.impl.ConsumptionEntry;
import io.github.ratelimit.storage.command.StorageBackendCommandResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DistributedEntryStorage implements EntryStorage {

    private static final Logger LOGGER = Logger.getLogger(DistributedEntryStorage.class.getName());

    private static final long STORAGE_TIMEOUT = Long.parseLong(System.getProperty("distributedStorageBackendTimeout", "500"));

    protected final StorageBackend<String> storageBackend;

    public DistributedEntryStorage(StorageBackend<String> storageBackend) {
        this.storageBackend = storageBackend;
    }

    @Override
    public List<RateLimitEntry> getCurrentEntries(String identifier) throws RateLimiterException {
        Future<StorageBackendCommandResult<List<RateLimitEntry>>> entries = storageBackend
                .getRateLimitEntries(identifier);
        try {
            return handleAsyncResult(identifier, entries, () -> getFallbackEntries(identifier));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Something went wrong while getCurrentEntries for user: " + identifier, ex);
            throw new RateLimiterException(ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public List<RateLimitEntry> getUpdateEntries(String identifier) throws RateLimiterException {
        Future<StorageBackendCommandResult<List<RateLimitEntry>>> entries = storageBackend
                .getUpdateRateLimitEntries(identifier);
        try {
            return handleAsyncResult(identifier, entries, () -> getFallbackEntries(identifier));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Something went wrong while getUpdateEntries for user: " + identifier, ex);
            throw new RateLimiterException(ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public ConsumptionEntry conformRateLimits(String identifier, int requestWeight)
            throws RateLimiterException {
        Future<StorageBackendCommandResult<ConsumptionEntry>> commandResult = storageBackend
                .conformsRateLimitsWithStatus(identifier, requestWeight);
        try {
            return handleAsyncResult(identifier, commandResult, () -> ConsumptionEntry.conformant(0L));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Something went wrong while conformUpdateRateLimits for user: " + identifier, ex);
            throw new RateLimiterException(ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public void storeEntries(String identifier, List<RateLimitEntry> entries) throws RateLimiterException {
        try {
            storageBackend.storeEntries(identifier, entries).get(getExecutionTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Something went wrong while storingEntries for user: " + identifier, ex);
            throw new RateLimiterException(ex.getMessage(), ex.getCause());
        }
    }

    private <T> T handleAsyncResult(String identifier, Future<StorageBackendCommandResult<T>> futureResult, Supplier<? extends T>
            fallbackSupplier) throws Exception {
        StorageBackendCommandResult<T> commandResult = futureResult.get(getExecutionTimeout(), TimeUnit.MILLISECONDS);
        if (!commandResult.containsEntries()) {
            List<RateLimitEntry> fallbackEntries = getFallbackEntries(identifier);
            storeEntries(identifier, fallbackEntries);
            return fallbackSupplier.get();
        }
        return commandResult.getData();

    }

    @Override
    public long getExecutionTimeout() {
        return STORAGE_TIMEOUT;
    }

    protected List<RateLimitEntry> getFallbackEntries(String identifier) {
        return Collections.emptyList();
    }

}
