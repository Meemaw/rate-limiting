package io.github.ratelimit.storage.command;

import io.github.ratelimit.core.RateLimitEntry;

import java.time.Instant;
import java.util.List;

public class GetEntries implements EntryStorageCommand<List<RateLimitEntry>> {

    private static final long serialVersionUID = -7951581530297533094L;

    @Override
    public List<RateLimitEntry> execute(List<RateLimitEntry> state, Instant currentTime) {
        return state;
    }

    @Override
    public boolean doesMutate() {
        return false;
    }

}
