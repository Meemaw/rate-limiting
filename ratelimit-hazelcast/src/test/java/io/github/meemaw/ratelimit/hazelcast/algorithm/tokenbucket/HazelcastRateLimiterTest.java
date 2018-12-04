package io.github.meemaw.ratelimit.hazelcast.algorithm.tokenbucket;

import com.hazelcast.core.Hazelcast;
import io.github.meemaw.ratelimit.hazelcast.HazelcastStorage;
import io.github.ratelimit.core.*;
import io.github.ratelimit.core.impl.ConsumptionEntry;
import io.github.ratelimit.core.impl.EntryBuilder;
import io.github.ratelimit.core.impl.RateLimiterImpl;
import io.github.ratelimit.core.impl.SimpleRefillPolicy;
import io.github.ratelimit.storage.DistributedEntryStorage;
import io.github.ratelimit.storage.StorageBackend;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HazelcastRateLimiterTest {

    protected RateLimiter rateLimiter;
    protected StorageBackend<String> storageBackend;

    protected static final String USER_ONE_POLICY = "u1";
    protected static final String MULTIPLE_USER_POLICY = "u2";
    protected static final String BAN_USER_POLICY = "u3";
    protected static final String EMPTY_ENTRIES_POLICY = "u4";


    @Rule
    public ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() {
            System.out.println("Setting up resource");
            storageBackend = new HazelcastStorage(Hazelcast.newHazelcastInstance());
            rateLimiter = new RateLimiterImpl(new DistributedEntryStorage(storageBackend));
        }

        @Override
        protected void after() {
            System.out.println("Cleaning up resource");
        }
    };


    @Before
    public void setup() {
        EntryBuilder builder = RateLimiting.entryBuilder().withAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        setupOnePolicyUser(builder);
        setupMultiplePolicyUser(builder);
        setupBannedPolicyUser(builder);
        setupEmptyMultipleEntries(builder);
    }

    private void setupOnePolicyUser(EntryBuilder builder) {
        List<RateLimitEntry> entries = Arrays.asList(builder.withRefillPolicy(SimpleRefillPolicy.perSecond(5)).build());
        storageBackend.storeEntries(USER_ONE_POLICY, entries);
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

    private void setupEmptyMultipleEntries(EntryBuilder builder) {
        RefillPolicy perSecondPolicy = SimpleRefillPolicy.perSecond(5);
        RefillPolicy perMinutePolicy = SimpleRefillPolicy.perMinute(10);
        RefillPolicy perHourPolicy = SimpleRefillPolicy.perHour(20);

        List<RateLimitEntry> entries = Arrays.asList(builder.withRefillPolicy(perMinutePolicy).build(),
                builder.withRefillPolicy(perHourPolicy).build(), builder.withRefillPolicy(perSecondPolicy).build());
        storageBackend.storeEntries(EMPTY_ENTRIES_POLICY, entries);
    }

    private void setupMultiplePolicyUser(EntryBuilder builder) {
        RefillPolicy perSecondPolicy = SimpleRefillPolicy.perSecond(5);
        RefillPolicy perMinutePolicy = SimpleRefillPolicy.perMinute(10);
        RefillPolicy perHourPolicy = SimpleRefillPolicy.perHour(20);

        List<RateLimitEntry> entries = Arrays.asList(builder.withRefillPolicy(perMinutePolicy).filled().build(),
                builder.withRefillPolicy(perSecondPolicy).filled().build(),
                builder.withRefillPolicy(perHourPolicy).filled().build());
        storageBackend.storeEntries(MULTIPLE_USER_POLICY, entries);
    }

    private void setupBannedPolicyUser(EntryBuilder builder) {
        RefillPolicy perHourPolicy = SimpleRefillPolicy.perHour(1000);
        List<RateLimitEntry> entries = Arrays.asList(builder.withRefillPolicy(perHourPolicy).filled().build(),
                builder.withRefillPolicy(SimpleRefillPolicy.banPolicy()).filled().build());
        storageBackend.storeEntries(BAN_USER_POLICY, entries);
    }

    @Test
    public void conformsOnNoEntriesReturn() throws RateLimiterException, InterruptedException, ExecutionException {
        assertTrue(rateLimiter.conformsRateLimits("test"));
        assertFalse(storageBackend.getRateLimitEntries("test").get().containsEntries());
    }

    @Test
    public void getStatsShouldUpdateRecords() throws InterruptedException, ExecutionException, RateLimiterException {
        RateLimitEntry initialEntry = storageBackend.getRateLimitEntries(USER_ONE_POLICY).get().getData().get(0);
        assertEquals(initialEntry.getTokenCount(), 0);
        assertEquals(rateLimiter.getTokenCounts(USER_ONE_POLICY).get("PT1S"), Long.valueOf(0));
        BlockingStrategy.SLEEPING.block(200000000L); // 0.2 seconds
        assertEquals(rateLimiter.getTokenCounts(USER_ONE_POLICY).get("PT1S"), Long.valueOf(1));
        BlockingStrategy.SLEEPING.block(200000000L); // 0.2 seconds
        assertEquals(rateLimiter.getTokenCounts(USER_ONE_POLICY).get("PT1S"), Long.valueOf(2));
        BlockingStrategy.SLEEPING.block(200000000L); // 0.2 seconds
        assertEquals(rateLimiter.getTokenCounts(USER_ONE_POLICY).get("PT1S"), Long.valueOf(3));
        BlockingStrategy.SLEEPING.block(200000000L); // 0.2 seconds
        assertEquals(rateLimiter.getTokenCounts(USER_ONE_POLICY).get("PT1S"), Long.valueOf(4));
        BlockingStrategy.SLEEPING.block(200000000L); // 0.2 seconds
        assertEquals(rateLimiter.getTokenCounts(USER_ONE_POLICY).get("PT1S"), Long.valueOf(5));
        BlockingStrategy.SLEEPING.block(200000000L); // 0.2 seconds
        assertEquals(rateLimiter.getTokenCounts(USER_ONE_POLICY).get("PT1S"), Long.valueOf(5));
    }

    @Test
    public void consumesTokensOneByOne() throws InterruptedException, ExecutionException, RateLimiterException {
        RateLimitEntry initialEntry = storageBackend.getRateLimitEntries(USER_ONE_POLICY).get().getData().get(0);
        assertEquals(initialEntry.getTokenCount(), 0);
        waitForFullEntryRefill(initialEntry);

        RateLimitEntry entry = storageBackend.getRateLimitEntries(USER_ONE_POLICY).get().getData().get(0);
        for (int i = 0; i < entry.getRefillPolicy().getCapacity(); i++) {
            assertTrue(rateLimiter.conformsRateLimits(USER_ONE_POLICY));
        }
        assertFalse(rateLimiter.conformsRateLimits(USER_ONE_POLICY));
    }

    @Test
    public void doesntConform_onEmptyBucket_butConformAfterRefill()
            throws InterruptedException, ExecutionException, RateLimiterException {
        assertEquals(storageBackend.getRateLimitEntries(USER_ONE_POLICY).get().getData().get(0).getTokenCount(), 0);
        assertFalse(rateLimiter.conformsRateLimits(USER_ONE_POLICY));
        BlockingStrategy.SLEEPING.block(100000000); // 0.1 seconds
        assertFalse(rateLimiter.conformsRateLimits(USER_ONE_POLICY));
        BlockingStrategy.SLEEPING.block(100000000); // 0.1 second
        assertTrue(rateLimiter.conformsRateLimits(USER_ONE_POLICY));
        assertFalse(rateLimiter.conformsRateLimits(USER_ONE_POLICY));
    }

    @Test
    public void refillToRefillCapacity_andConform()
            throws InterruptedException, ExecutionException, RateLimiterException {
        RateLimitEntry initialEntry = storageBackend.getRateLimitEntries(USER_ONE_POLICY).get().getData().get(0);
        assertEquals(initialEntry.getTokenCount(), 0);
        waitForFullEntryRefill(initialEntry);

        assertTrue(rateLimiter.conformsRateLimits(USER_ONE_POLICY, 1));
        RateLimitEntry entry = storageBackend.getRateLimitEntries(USER_ONE_POLICY).get().getData().get(0);
        assertEquals(entry.getTokenCount(), entry.getRefillPolicy().getCapacity() - 1);
    }

    @Test
    public void consumptionEntry_isCorrectlyConsumed()
            throws InterruptedException, ExecutionException, RateLimiterException {
        RateLimitEntry initialEntry = storageBackend.getRateLimitEntries(USER_ONE_POLICY).get().getData().get(0);
        waitForFullEntryRefill(initialEntry);

        ConsumptionEntry consumptionEntry = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        assertEquals(consumptionEntry.getRemainingTokens(), 4);
        assertEquals(consumptionEntry.doesConform(), true);

        consumptionEntry = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        assertEquals(consumptionEntry.getRemainingTokens(), 3);
        assertEquals(consumptionEntry.doesConform(), true);

        consumptionEntry = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        assertEquals(consumptionEntry.getRemainingTokens(), 2);
        assertEquals(consumptionEntry.doesConform(), true);
        consumptionEntry = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        assertEquals(consumptionEntry.getRemainingTokens(), 1);
        assertEquals(consumptionEntry.doesConform(), true);
        consumptionEntry = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        assertEquals(consumptionEntry.getRemainingTokens(), 0);
        assertEquals(consumptionEntry.doesConform(), true);

        assertFalse(rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY).doesConform());
        assertFalse(rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY).doesConform());

        consumptionEntry = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        assertEquals(consumptionEntry.getRemainingTokens(), 0);
        assertEquals(consumptionEntry.doesConform(), false);
        assertTrue(consumptionEntry.getNanosUntilConsumption() > 165000000);
    }

    @Test
    public void consumptionEntry_notPresent_whenNoPolicies() throws RateLimiterException {
        ConsumptionEntry consumptionEntry = rateLimiter.conformRateLimitsWithConsumption("randomIdentifier");
        assertEquals(consumptionEntry.doesConform(), true);
        assertEquals(consumptionEntry.getNanosUntilConsumption(), 0);
        assertEquals(consumptionEntry.getRemainingTokens(), 0);
    }

    @Test
    public void multiplePolicy_shouldConform_untillAllConforms() throws InterruptedException, ExecutionException, RateLimiterException {
        conformsTimes(MULTIPLE_USER_POLICY, 5);

        assertFalse(rateLimiter.conformsRateLimits(MULTIPLE_USER_POLICY));
        RateLimitEntry perSecondEntry = storageBackend.getRateLimitEntries(MULTIPLE_USER_POLICY).get().getData().get(1);
        assertEquals(perSecondEntry.getTokenCount(), 0);
        waitForFullEntryRefill(perSecondEntry);

        conformsTimes(MULTIPLE_USER_POLICY, 5);

        waitForFullEntryRefill(perSecondEntry);
        assertFalse(rateLimiter.conformsRateLimits(MULTIPLE_USER_POLICY));
        perSecondEntry = storageBackend.getRateLimitEntries(MULTIPLE_USER_POLICY).get().getData().get(1);
        assertEquals(perSecondEntry.getTokenCount(), 5);
        RateLimitEntry perMinuteEntry = storageBackend.getRateLimitEntries(MULTIPLE_USER_POLICY).get().getData().get(0);
        assertEquals(perMinuteEntry.getTokenCount(), 0);

        RateLimitEntry perHourEntry = storageBackend.getRateLimitEntries(MULTIPLE_USER_POLICY).get().getData().get(2);
        assertEquals(perHourEntry.getTokenCount(), 10);
    }

    @Test
    public void consumptionEntry_shouldReturnSmallestTokenCount()
            throws InterruptedException, ExecutionException, RateLimiterException {
        ConsumptionEntry entry = rateLimiter.conformRateLimitsWithConsumption(MULTIPLE_USER_POLICY);
        assertEquals(entry.getRemainingTokens(), 4);
    }

    @Test
    public void bannedUser_neverConforms() throws InterruptedException, ExecutionException, RateLimiterException {
        ConsumptionEntry consumptionEntry = rateLimiter.conformRateLimitsWithConsumption(BAN_USER_POLICY);
        assertFalse(consumptionEntry.doesConform());
        assertEquals(consumptionEntry.getRemainingTokens(), 0);
        assertTrue(consumptionEntry.getNanosUntilConsumption() >= 922337203600000000L);
    }

    @Test
    public void testConsumptionEntryTimings() throws RateLimiterException, InterruptedException {
        waitForFullEntryRefill(rateLimiter.getStorage().getCurrentEntries(USER_ONE_POLICY).get(0));
        conformsTimes(USER_ONE_POLICY, 5);

        ConsumptionEntry notConformant = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        assertFalse(notConformant.doesConform());
        assertEquals(notConformant.getRemainingTokens(), 0);
        // Approx 0.2 seconds
        assertTrue(notConformant.getNanosUntilConsumption() >= 170000000
                && notConformant.getNanosUntilConsumption() <= 200000000);
        BlockingStrategy.SLEEPING.block(100000000);
        notConformant = rateLimiter.conformRateLimitsWithConsumption(USER_ONE_POLICY);
        // Approx 0.1 second
        assertTrue(notConformant.getNanosUntilConsumption() >= 60000000
                && notConformant.getNanosUntilConsumption() <= 100000000);
    }

    @Test
    public void returnsConsumptionWithLowestTokenCount() throws RateLimiterException, InterruptedException {
        ConsumptionEntry entry = rateLimiter.conformRateLimitsWithConsumption(MULTIPLE_USER_POLICY);
        assertEquals(entry.getRemainingTokens(), 4);
    }

    @Test
    public void getNanosUntillRefillFromSlowestPolicy() throws RateLimiterException, InterruptedException {
        ConsumptionEntry entry = rateLimiter.conformRateLimitsWithConsumption(EMPTY_ENTRIES_POLICY);
        assertEquals(entry.getRemainingTokens(), 0);
        assertFalse(entry.doesConform());
        // Slowest policy 20req/hour -> token every 180 second
        // getNanosUntilConsumption() should be between 179 and 180 seconds
        assertTrue(
                entry.getNanosUntilConsumption() <= 180000000000L && entry.getNanosUntilConsumption() >= 179000000000L);
    }

}
