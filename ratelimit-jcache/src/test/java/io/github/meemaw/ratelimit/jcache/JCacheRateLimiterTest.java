package io.github.meemaw.ratelimit.jcache;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimiter;
import io.github.ratelimit.core.RateLimiterException;
import io.github.ratelimit.core.impl.RateLimiterImpl;
import io.github.ratelimit.storage.DistributedEntryStorage;
import io.github.ratelimit.storage.StorageBackend;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class JCacheRateLimiterTest {

    private CachingProvider cachingProvider = Caching.getCachingProvider();
    private CacheManager cacheManager = cachingProvider.getCacheManager();

    protected RateLimiter rateLimiter;
    protected StorageBackend<String> storageBackend;


    @Before
    public void setup() {
        MutableConfiguration<String, List<RateLimitEntry>> config = new MutableConfiguration<>();
        Cache<String, List<RateLimitEntry>> cache = cacheManager.createCache("rateLimitCache", config);
        storageBackend = new JCacheStorage<>(cache);
        rateLimiter = RateLimiterImpl.withStorage(new DistributedEntryStorage(storageBackend));
    }


    @After
    public void tearDown() {
        cacheManager.destroyCache("rateLimitCache");
    }

    @Test
    public void conformsOnNoEntriesReturn() throws RateLimiterException, InterruptedException, ExecutionException {
        assertTrue(rateLimiter.conformsRateLimits("test"));
        assertFalse(storageBackend.getRateLimitEntries("test").get().containsEntries());
    }


}
