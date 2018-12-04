package io.github.ratelimit.core;

import io.github.ratelimit.storage.InMemoryStorageBackend;
import io.github.ratelimit.storage.StorageBackend;

public abstract class AbstractInMemoryRateLimiterTest extends AbstractRateLimiterTest {

    @Override
    protected StorageBackend<String> getBackendStorage() {
        return new InMemoryStorageBackend<>();
    }

}
