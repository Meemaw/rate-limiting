package io.github.ratelimit.storage.command;

import io.github.ratelimit.core.RateLimitEntry;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public interface EntryStorageCommand<T> extends Serializable {

    T execute(List<RateLimitEntry> entries, Instant currentTime);

    boolean doesMutate();

}
