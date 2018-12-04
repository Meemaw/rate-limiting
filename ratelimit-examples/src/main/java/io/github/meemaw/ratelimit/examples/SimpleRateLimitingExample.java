package io.github.meemaw.ratelimit.examples;

import io.github.ratelimit.core.EntryStorage;
import io.github.ratelimit.core.RateLimiter;
import io.github.ratelimit.core.RateLimiterException;
import io.github.ratelimit.core.RateLimiting;
import io.github.ratelimit.storage.DistributedEntryStorage;
import io.github.ratelimit.storage.InMemoryStorageBackend;
import io.github.ratelimit.storage.StorageBackend;

public class SimpleRateLimitingExample {


    public static void main(String[] args) throws RateLimiterException {
        StorageBackend<String> storageBackend = new InMemoryStorageBackend<>(); // in memory impl.
        EntryStorage entryStorage = new DistributedEntryStorage(storageBackend); // async mode
        RateLimiter rateLimiter = RateLimiting.withStorage(entryStorage);

        if (rateLimiter.conformsRateLimits("userIdentifier")) {
            System.out.println("User has no policies so this will be printed!");
        } else {
            System.out.println("Too many requests!");
        }
    }
}
