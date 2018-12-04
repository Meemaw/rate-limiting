package io.github.ratelimit.core;

import io.github.ratelimit.core.impl.EntryBuilder;
import io.github.ratelimit.core.impl.RateLimiterImpl;
import io.github.ratelimit.core.impl.SimpleRefillPolicy;
import io.github.ratelimit.storage.DistributedEntryStorage;
import io.github.ratelimit.storage.StorageBackend;
import org.junit.Before;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public abstract class AbstractRateLimiterTest {


    protected RateLimiter rateLimiter;
    protected StorageBackend<String> storageBackend;

    protected abstract StorageBackend<String> getBackendStorage();

    protected abstract RateLimitAlgorithm getAlgorithm();

    protected static final String USER_ONE_POLICY = "u1";
    protected static final String MULTIPLE_USER_POLICY = "u2";
    protected static final String BAN_USER_POLICY = "u3";


    @Before
    public void setup() {
        storageBackend = getBackendStorage();
        rateLimiter = new RateLimiterImpl(new DistributedEntryStorage(storageBackend));
        EntryBuilder builder = RateLimiting.entryBuilder().withAlgorithm(getAlgorithm());
        setupOnePolicyUser(builder);
        setupMultiplePolicyUser(builder);
        setupBannedPolicyUser(builder);
    }

    private void setupOnePolicyUser(EntryBuilder builder) {
        List<RateLimitEntry> entries = Arrays.asList(builder.withRefillPolicy(SimpleRefillPolicy.perSecond(5)).build());
        storageBackend.storeEntries(USER_ONE_POLICY, entries);
    }

    private void setupMultiplePolicyUser(EntryBuilder builder) {
        RefillPolicy perSecondPolicy = SimpleRefillPolicy.perSecond(5);
        RefillPolicy perMinutePolicy = SimpleRefillPolicy.perMinute(10);
        RefillPolicy perHourPolicy = SimpleRefillPolicy.perHour(20);

        List<RateLimitEntry> entries = Arrays.asList(builder.withRefillPolicy(perSecondPolicy).filled().build(),
                builder.withRefillPolicy(perMinutePolicy).filled().build(),
                builder.withRefillPolicy(perHourPolicy).filled().build());
        storageBackend.storeEntries(MULTIPLE_USER_POLICY, entries);
    }

    private void setupBannedPolicyUser(EntryBuilder builder) {
        RefillPolicy perHourPolicy = SimpleRefillPolicy.perHour(1000);
        List<RateLimitEntry> entries = Arrays.asList(builder.withRefillPolicy(perHourPolicy).filled().build(),
                builder.withRefillPolicy(SimpleRefillPolicy.banPolicy()).filled().build());
        storageBackend.storeEntries(BAN_USER_POLICY, entries);
    }

    protected void waitForFullEntryRefill(RateLimitEntry entry) throws InterruptedException {
        long missingTokens = entry.getMissingTokens();
        long nanosBetweenRefills = entry.getRefillPolicy().getNanosBetweenRefills();
        BlockingStrategy.SLEEPING.block(missingTokens * nanosBetweenRefills);
    }

    protected void conformsTimes(String identifier, int n) throws RateLimiterException {
        for (int i = 0; i < n; i++) {
            assertTrue(rateLimiter.conformsRateLimits(identifier));
        }
    }

    public StorageBackend<String> getStorageBackend() {
        return storageBackend;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }


}
