package io.github.ratelimit.storage.command;

import io.github.ratelimit.core.RateLimitEntry;

import java.time.Instant;
import java.util.List;

public class GetUpdateEntries implements EntryStorageCommand<List<RateLimitEntry>> {

    private static final long serialVersionUID = 4839066595667560613L;

    @Override
    public List<RateLimitEntry> execute(List<RateLimitEntry> entries, Instant currentTime) {
        for (RateLimitEntry entry : entries) {
            entry.updateInPlace(currentTime);
        }
        return entries;
    }

    @Override
    public boolean doesMutate() {
        return true;
    }

}
