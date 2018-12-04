package io.github.ratelimit.storage;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.storage.command.EntryStorageCommand;
import io.github.ratelimit.storage.command.StorageBackendCommandResult;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class RateLimitEntryProcessor<K extends Serializable, T>
        implements Serializable, EntryProcessor<K, List<RateLimitEntry>, StorageBackendCommandResult<T>> {

    private static final long serialVersionUID = -1972118181327639994L;

    private EntryStorageCommand<T> targetCommand;

    public RateLimitEntryProcessor(EntryStorageCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    @Override
    public StorageBackendCommandResult<T> process(MutableEntry<K, List<RateLimitEntry>> mutableEntry, Object... rest)
            throws EntryProcessorException {
        if (!mutableEntry.exists()) {
            return StorageBackendCommandResult.entriesNotFound();
        }
        List<RateLimitEntry> entries = mutableEntry.getValue();
        if (entries.isEmpty()) {
            return StorageBackendCommandResult.entriesNotFound();
        }
        T result = targetCommand.execute(entries, Instant.now());

        if (targetCommand.doesMutate()) {
            mutableEntry.setValue(entries);
        }
        return StorageBackendCommandResult.success(result);
    }

}
