package io.github.meemaw.ratelimit.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import io.github.ratelimit.core.RateLimitEntry;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class SimpleBackupProcessor<K extends Serializable> implements EntryBackupProcessor<K, List<RateLimitEntry>> {

    private static final long serialVersionUID = 1L;

    private final List<RateLimitEntry> state;

    public SimpleBackupProcessor(List<RateLimitEntry> state) {
        this.state = state;
    }

    @Override
    public void processBackup(Map.Entry<K, List<RateLimitEntry>> entry) {
        entry.setValue(state);
    }

}
