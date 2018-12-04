package io.github.ratelimit.algorithm.slidingwindowlog;

import io.github.ratelimit.algorithm.SlidingWindowLogRecord;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RecordTest {

    @Test
    public void canUpdateRecordToFullCapacity() {
        Instant now = Instant.now();
        long initialTokenCount = 3;
        RateLimitRecord slidingWindowLogRecord = SlidingWindowLogRecord.of(initialTokenCount, REFILL_FIVE_PER_SECOND.getCapacity());
        RateLimitRecord updated = slidingWindowLogRecord.updateWith(REFILL_FIVE_PER_SECOND, now.plus(REFILL_FIVE_PER_SECOND.getSamplingPeriod().plus(Duration.ofMillis(1))));
        assertEquals(updated.getTokenCount(), REFILL_FIVE_PER_SECOND.getCapacity());
        RateLimitRecord updatedAfter = slidingWindowLogRecord.updateWith(REFILL_FIVE_PER_SECOND, now.plus(Duration.ofDays(1000)));
        assertEquals(updatedAfter.getTokenCount(), REFILL_FIVE_PER_SECOND.getCapacity());
    }

    @Test
    public void shouldBeAbleToTryConsumeWholeTokenCount() {
        long initialTokenCount = 3;
        RateLimitRecord slidingWindowLogRecord = SlidingWindowLogRecord.of(initialTokenCount, REFILL_FIVE_PER_SECOND.getCapacity());
        RateLimitEntry entry = new NonBlockingEntry(slidingWindowLogRecord, REFILL_FIVE_PER_SECOND);
        assertTrue(entry.tryConsume(initialTokenCount));
        assertFalse(entry.tryConsume(1));
    }

    @Test
    public void shouldBeAbleToConsumeWholeTokenCount() {
        long initialTokenCount = 3;
        RateLimitRecord slidingWindowLogRecord = SlidingWindowLogRecord.of(initialTokenCount, REFILL_FIVE_PER_SECOND.getCapacity());
        RateLimitEntry entry = new NonBlockingEntry(slidingWindowLogRecord, REFILL_FIVE_PER_SECOND);
        assertTrue(entry.canConsume(initialTokenCount));
        assertFalse(entry.canConsume(initialTokenCount + 1));
    }

    @Test
    public void testTryConsumeDoesntConsumeTokensWhenFail() {
        long initialTokenCount = 3;
        RateLimitRecord slidingWindowLogRecord = SlidingWindowLogRecord.of(initialTokenCount, REFILL_FIVE_PER_SECOND.getCapacity());
        RateLimitEntry entry = new NonBlockingEntry(slidingWindowLogRecord, REFILL_FIVE_PER_SECOND);
        assertFalse(entry.tryConsume(initialTokenCount + 1));
        assertEquals(entry.getTokenCount(), initialTokenCount);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenConsumptionExcedsPolicyLimits() {
        RateLimitEntry entry = RateLimiting.schedulerBuilder().withRefillPolicy(REFILL_TEN_PER_SECOND)
                .withAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW).withThrowOnPolicyExceds().build();
        entry.tryConsumeAndReturnRemaining(20);
    }

    @Test
    public void testConsumptionEntry() throws InterruptedException {
        long initialTokenCount = 3;
        RateLimitRecord slidingWindowLogRecord = SlidingWindowLogRecord.of(initialTokenCount, REFILL_TEN_PER_SECOND.getCapacity());
        RateLimitEntry entry = new NonBlockingEntry(slidingWindowLogRecord, REFILL_TEN_PER_SECOND);
        ConsumptionEntry consumptionEntry = entry.tryConsumeAndReturnRemaining(initialTokenCount + 1);
        assertFalse(consumptionEntry.doesConform());
        entry = entry.updateInPlace(Instant.now().plus(Duration.ofNanos(consumptionEntry.getNanosUntilConsumption() + 1)));
        consumptionEntry = entry.tryConsumeAndReturnRemaining(initialTokenCount + 1);
        assertTrue(consumptionEntry.doesConform());
        assertEquals(consumptionEntry.getRemainingTokens(), REFILL_TEN_PER_SECOND.getCapacity() - (initialTokenCount + 1));
    }

}
