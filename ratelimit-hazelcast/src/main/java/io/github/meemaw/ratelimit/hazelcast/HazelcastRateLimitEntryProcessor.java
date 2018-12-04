package io.github.meemaw.ratelimit.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.storage.RateLimitEntryProcessor;
import io.github.ratelimit.storage.command.StorageBackendCommandResult;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class HazelcastRateLimitEntryProcessor<K extends Serializable, T> implements EntryProcessor<K, List<RateLimitEntry>> {

    private static final long serialVersionUID = -725516689492141419L;

    private final RateLimitEntryProcessor<K, T> entryProcessor;
    private EntryBackupProcessor<K, List<RateLimitEntry>> backupProcessor;

    public HazelcastRateLimitEntryProcessor(RateLimitEntryProcessor<K, T> entryProcessor) {
        this.entryProcessor = entryProcessor;
    }

    @Override
    public EntryBackupProcessor<K, List<RateLimitEntry>> getBackupProcessor() {
        return backupProcessor;
    }

    @Override
    public Object process(Map.Entry<K, List<RateLimitEntry>> mapEntry) {
        HazelcastMutableEntry<K> entryAdapter = new HazelcastMutableEntry<>(mapEntry);
        StorageBackendCommandResult<T> result = entryProcessor.process(entryAdapter);
        if (entryAdapter.isModified()) {
            backupProcessor = new SimpleBackupProcessor<>(mapEntry.getValue());
        }
        return result;
    }

}
