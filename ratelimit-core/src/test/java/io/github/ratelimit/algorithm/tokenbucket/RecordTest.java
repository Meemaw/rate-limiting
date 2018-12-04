package io.github.ratelimit.algorithm.tokenbucket;

import io.github.ratelimit.algorithm.TokenBucketRecord;
import io.github.ratelimit.core.RateLimitAlgorithm;
import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimitRecord;
import io.github.ratelimit.core.RateLimiting;
import io.github.ratelimit.core.impl.ConsumptionEntry;
import io.github.ratelimit.core.impl.NonBlockingEntry;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static io.github.ratelimit.core.RateLimitTestConstants.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;


public class RecordTest {

    @Test
    public void canUpdateRecordToFullCapacity() {
        Instant now = Instant.now();
        long initialTokenCount = 3;
        RateLimitRecord tokenBucketRecord = TokenBucketRecord.of(initialTokenCount, now.plus(DURATION_HALF_SECOND));
        RateLimitRecord updated = tokenBucketRecord.updateWith(REFILL_FIVE_PER_SECOND, now.plus(DURATION_ONE_SECOND));
        assertEquals(updated.getTokenCount(), REFILL_FIVE_PER_SECOND.getCapacity());
        RateLimitRecord updatedAfter = tokenBucketRecord.updateWith(REFILL_FIVE_PER_SECOND, now.plus(Duration.ofDays(1000)));
        assertEquals(updatedAfter.getTokenCount(), REFILL_FIVE_PER_SECOND.getCapacity());
    }

    @Test
    public void shouldBeAbleToTryConsumeWholeTokenCount() {
        long initialTokenCount = 3;
        RateLimitRecord tokenBucketRecord = TokenBucketRecord.of(initialTokenCount, Instant.now().plus(DURATION_HALF_SECOND));
        RateLimitEntry entry = new NonBlockingEntry(tokenBucketRecord, REFILL_FIVE_PER_SECOND);
        assertTrue(entry.tryConsume(initialTokenCount));
        assertFalse(entry.tryConsume(1));
    }

    @Test
    public void shouldBeAbleToConsumeWholeTokenCount() {
        long initialTokenCount = 3;
        RateLimitRecord tokenBucketRecord = TokenBucketRecord.of(initialTokenCount, Instant.now().plus(DURATION_HALF_SECOND));
        RateLimitEntry entry = new NonBlockingEntry(tokenBucketRecord, REFILL_FIVE_PER_SECOND);
        assertTrue(entry.canConsume(initialTokenCount));
        assertFalse(entry.canConsume(initialTokenCount + 1));
    }

    @Test
    public void testTryConsumeDoesntConsumeTokensWhenFail() {
        long initialTokenCount = 3;
        RateLimitRecord tokenBucketRecord = TokenBucketRecord.of(initialTokenCount, Instant.now().plus(DURATION_HALF_SECOND));
        RateLimitEntry entry = new NonBlockingEntry(tokenBucketRecord, REFILL_FIVE_PER_SECOND);
        assertFalse(entry.tryConsume(initialTokenCount + 1));
        assertEquals(entry.getTokenCount(), initialTokenCount);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenConsumptionExcedsPolicyLimits() {
        RateLimitEntry entry = RateLimiting.schedulerBuilder().withRefillPolicy(REFILL_TEN_PER_SECOND)
                .withAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET).withThrowOnPolicyExceds().build();
        entry.tryConsumeAndReturnRemaining(20);
    }

    @Test
    public void testConsumptionEntry() throws InterruptedException {
        long initialTokenCount = 3;
        Instant now = Instant.now();
        RateLimitRecord tokenBucketRecord = TokenBucketRecord.of(initialTokenCount, now);
        RateLimitEntry entry = new NonBlockingEntry(tokenBucketRecord, REFILL_TEN_PER_SECOND);
        ConsumptionEntry consumptionEntry = entry.tryConsumeAndReturnRemaining(initialTokenCount + 1);
        assertFalse(consumptionEntry.doesConform());
        entry = entry.updateInPlace(Instant.now().plus(Duration.ofNanos(consumptionEntry.getNanosUntilConsumption())));
        consumptionEntry = entry.tryConsumeAndReturnRemaining(initialTokenCount + 1);
        assertTrue(consumptionEntry.doesConform());
        assertEquals(consumptionEntry.getRemainingTokens(), 0);
    }

    @Test
    public void testEquals() {
        Instant now = Instant.now();
        assertEquals(TokenBucketRecord.of(5, now), TokenBucketRecord.of(5, now));
        assertNotEquals(TokenBucketRecord.of(6, now), TokenBucketRecord.of(5, now));
        assertNotEquals(TokenBucketRecord.of(6, Instant.now().plus(Duration.ofMillis(1))),
                TokenBucketRecord.of(6, Instant.now()));
    }
}
