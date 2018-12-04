package io.github.ratelimit.algorithm.tokenbucket;

import io.github.ratelimit.core.*;
import io.github.ratelimit.core.impl.ConsumptionEntry;
import org.junit.Test;


import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class RateLimiterInMemoryTest extends AbstractInMemoryRateLimiterTest {

    @Override
    protected RateLimitAlgorithm getAlgorithm() {
        return RateLimitAlgorithm.TOKEN_BUCKET;
    }

    @Test
    public void conformsOnUserWithNoEntries() throws RateLimiterException, InterruptedException, ExecutionException {
        assertTrue(rateLimiter.conformsRateLimits("test"));
        assertEquals(storageBackend.getRateLimitEntries("test").get().getData().size(), 0);
    }


    @Test
    public void getStatsShouldUpdateRecords() throws ExecutionException, InterruptedException, RateLimiterException {
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
        assertTrue(consumptionEntry.getNanosUntilConsumption() > 180000000);
    }

    @Test
    public void consumptionEntry_notPresent_whenNoPolicies() throws RateLimiterException {
        ConsumptionEntry consumptionEntry = rateLimiter.conformRateLimitsWithConsumption("randomIdentifier");
        assertEquals(consumptionEntry.doesConform(), true);
        assertEquals(consumptionEntry.getNanosUntilConsumption(), 0);
        assertEquals(consumptionEntry.getRemainingTokens(), 0);
    }

    @Test
    public void multiplePolicy_shouldConform_untillAllConforms()
            throws InterruptedException, ExecutionException, RateLimiterException {
        conformsTimes(MULTIPLE_USER_POLICY, 5);

        assertFalse(rateLimiter.conformsRateLimits(MULTIPLE_USER_POLICY));
        RateLimitEntry perSecondEntry = storageBackend.getRateLimitEntries(MULTIPLE_USER_POLICY).get().getData().get(0);
        assertEquals(perSecondEntry.getTokenCount(), 0);
        waitForFullEntryRefill(perSecondEntry);

        conformsTimes(MULTIPLE_USER_POLICY, 5);

        waitForFullEntryRefill(perSecondEntry);
        assertFalse(rateLimiter.conformsRateLimits(MULTIPLE_USER_POLICY));
        perSecondEntry = storageBackend.getRateLimitEntries(MULTIPLE_USER_POLICY).get().getData().get(0);
        assertEquals(perSecondEntry.getTokenCount(), 5);
        RateLimitEntry perMinuteEntry = storageBackend.getRateLimitEntries(MULTIPLE_USER_POLICY).get().getData().get(1);
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



}
