package io.github.meemaw.ratelimit.hazelcast;

import io.github.ratelimit.core.RateLimitEntry;

import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class HazelcastMutableEntry<K extends Serializable> implements MutableEntry<K, List<RateLimitEntry>> {

    private final Map.Entry<K, List<RateLimitEntry>> entry;
    private boolean modified;


    public HazelcastMutableEntry(Map.Entry<K, List<RateLimitEntry>> entry) {
        this.entry = entry;
    }

    @Override
    public boolean exists() {
        return entry.getValue() != null;
    }

    @Override
    public void remove() {
        entry.setValue(null);
    }

    @Override
    public void setValue(List<RateLimitEntry> value) {
        entry.setValue(value);
        this.modified = true;
    }

    @Override
    public K getKey() {
        return entry.getKey();
    }

    @Override
    public List<RateLimitEntry> getValue() {
        return entry.getValue();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    public boolean isModified() {
        return modified;
    }

}
