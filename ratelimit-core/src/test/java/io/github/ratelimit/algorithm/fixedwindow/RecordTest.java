package io.github.ratelimit.algorithm.fixedwindow;

import io.github.ratelimit.algorithm.FixedWindowRecord;
import io.github.ratelimit.core.*;
import io.github.ratelimit.core.impl.ConsumptionEntry;
import io.github.ratelimit.core.impl.NonBlockingEntry;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static io.github.ratelimit.core.RateLimitTestConstants.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class RecordTest {

    @Test
    public void canUpdateRecordToFullCapacity() {
        Instant now = Instant.now();
        long initialTokenCount = 3;
        RateLimitRecord fixedWindowRecord = FixedWindowRecord.of(initialTokenCount, now.plus(DURATION_HALF_SECOND));
        RateLimitRecord updated = fixedWindowRecord.updateWith(REFILL_FIVE_PER_SECOND, now.plus(DURATION_ONE_SECOND));
        assertEquals(updated.getTokenCount(), REFILL_FIVE_PER_SECOND.getCapacity());
        RateLimitRecord updatedAfter = fixedWindowRecord.updateWith(REFILL_FIVE_PER_SECOND, now.plus(Duration.ofDays(1000)));
        assertEquals(updatedAfter.getTokenCount(), REFILL_FIVE_PER_SECOND.getCapacity());
    }

    @Test
    public void shouldBeAbleToTryConsumeWholeTokenCount() {
        long initialTokenCount = 3;
        RateLimitRecord fixedWindowRecord = FixedWindowRecord.of(initialTokenCount, Instant.now().plus(DURATION_HALF_SECOND));
        RateLimitEntry entry = new NonBlockingEntry(fixedWindowRecord, REFILL_FIVE_PER_SECOND);
        assertTrue(entry.tryConsume(initialTokenCount));
        assertFalse(entry.tryConsume(1));
    }

    @Test
    public void shouldBeAbleToConsumeWholeTokenCount() {
        long initialTokenCount = 3;
        RateLimitRecord fixedWindowRecord = FixedWindowRecord.of(initialTokenCount, Instant.now().plus(DURATION_HALF_SECOND));
        RateLimitEntry entry = new NonBlockingEntry(fixedWindowRecord, REFILL_FIVE_PER_SECOND);
        assertTrue(entry.canConsume(initialTokenCount));
        assertFalse(entry.canConsume(initialTokenCount + 1));
    }

    @Test
    public void testTryConsumeDoesntConsumeTokensWhenFail() {
        long initialTokenCount = 3;
        RateLimitRecord fixedWindowRecord = FixedWindowRecord.of(initialTokenCount, Instant.now().plus(DURATION_HALF_SECOND));
        RateLimitEntry entry = new NonBlockingEntry(fixedWindowRecord, REFILL_FIVE_PER_SECOND);
        assertFalse(entry.tryConsume(initialTokenCount + 1));
        assertEquals(entry.getTokenCount(), initialTokenCount);
    }

    @Test(expected= IllegalArgumentException.class)
    public void throwsWhenConsumptionExcedsPolicyLimits() {
        RateLimitEntry entry = RateLimiting.schedulerBuilder().withRefillPolicy(REFILL_TEN_PER_SECOND)
                .withAlgorithm(RateLimitAlgorithm.FIXED_WINDOW).withThrowOnPolicyExceds().build();
        entry.tryConsumeAndReturnRemaining(20);
    }

    @Test
    public void testConsumptionEntry() throws InterruptedException {
        long initialTokenCount = 3;
        Instant now = Instant.now();
        RateLimitRecord fixedWindowRecord = FixedWindowRecord.of(initialTokenCount, now.plus(DURATION_ONE_SECOND));
        RateLimitEntry entry = new NonBlockingEntry(fixedWindowRecord, REFILL_TEN_PER_SECOND);
        ConsumptionEntry consumptionEntry = entry.tryConsumeAndReturnRemaining(initialTokenCount + 1);
        assertFalse(consumptionEntry.doesConform());
        entry.updateInPlace(now.plus(DURATION_ONE_SECOND).plus(Duration.ofMillis(1)));
        consumptionEntry = entry.tryConsumeAndReturnRemaining(initialTokenCount + 1);
        assertTrue(consumptionEntry.doesConform());
        assertEquals(consumptionEntry.getRemainingTokens(), REFILL_TEN_PER_SECOND.getCapacity() - (initialTokenCount + 1));
    }

    @Test
    public void testEquals() {
        Instant now = Instant.now();
        assertEquals(FixedWindowRecord.of(5, now), FixedWindowRecord.of(5, now));
        assertNotEquals(FixedWindowRecord.of(6, now), FixedWindowRecord.of(5, now));
        assertNotEquals(FixedWindowRecord.of(6, Instant.now().plus(Duration.ofMillis(1))),
                FixedWindowRecord.of(6, Instant.now()));
    }

}
