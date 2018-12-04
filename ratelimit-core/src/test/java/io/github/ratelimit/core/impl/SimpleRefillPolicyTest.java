package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.RefillPolicy;
import org.junit.Test;

import java.time.Duration;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class SimpleRefillPolicyTest {

    @Test
    public void secondTokenPolicyDistribution() {
        RefillPolicy secondPolicy = SimpleRefillPolicy.perSecond(20);

        assertEquals(20, secondPolicy.distributeNewTokens(Duration.ofSeconds(1)));
        assertEquals(10, secondPolicy.distributeNewTokens(Duration.ofMillis(500)));
        assertEquals(2, secondPolicy.distributeNewTokens(Duration.ofMillis(100)));
        assertEquals(1, secondPolicy.distributeNewTokens(Duration.ofMillis(99)));
    }

    @Test
    public void minuteTokenPolicyDistribution() {
        RefillPolicy minutePolicy = SimpleRefillPolicy.perMinute(10);

        assertEquals(1, minutePolicy.distributeNewTokens(Duration.ofSeconds(10)));
        assertEquals(1, minutePolicy.distributeNewTokens(Duration.ofSeconds(6)));
        assertEquals(0, minutePolicy.distributeNewTokens(Duration.ofSeconds(5)));
        assertEquals(2, minutePolicy.distributeNewTokens(Duration.ofSeconds(12)));
        assertEquals(10, minutePolicy.distributeNewTokens(Duration.ofSeconds(60)));
    }

    @Test
    public void hourTokenPolicyDistribution() {
        RefillPolicy hourPolicy = SimpleRefillPolicy.perHour(50);

        assertEquals(50, hourPolicy.distributeNewTokens(Duration.ofMinutes(60)));
        assertEquals(25, hourPolicy.distributeNewTokens(Duration.ofMinutes(30)));
        assertEquals(0, hourPolicy.distributeNewTokens(Duration.ofMinutes(1)));
        assertEquals(50, hourPolicy.distributeNewTokens(Duration.ofSeconds(3600)));
        assertEquals(49, hourPolicy.distributeNewTokens(Duration.ofSeconds(3599)));
    }

    @Test
    public void dayTokenPolicyDistribution() {
        RefillPolicy dayPolicy = SimpleRefillPolicy.perDay(100);

        assertEquals(100, dayPolicy.distributeNewTokens(Duration.ofDays(1)));
        assertEquals(50, dayPolicy.distributeNewTokens(Duration.ofHours(12)));
        assertEquals(1, dayPolicy.distributeNewTokens(Duration.ofSeconds(864)));
        assertEquals(0, dayPolicy.distributeNewTokens(Duration.ofSeconds(863)));
    }

    @Test
    public void testBanPolicy() {
        RefillPolicy banPolicy = SimpleRefillPolicy.banPolicy();
        assertEquals(banPolicy.getCapacity(), 0);
        assertEquals(banPolicy.getNanosBetweenRefills(), Long.MAX_VALUE);
        assertEquals(banPolicy.distributeNewTokens(Duration.ofHours(10)), 0);
    }

    @Test
    public void testEquals() {
        assertTrue(SimpleRefillPolicy.perDay(1).equals(SimpleRefillPolicy.perDay(1)));
        assertTrue(SimpleRefillPolicy.perMinute(1).equals(SimpleRefillPolicy.perMinute(1)));
        assertFalse(SimpleRefillPolicy.perMinute(1).equals(SimpleRefillPolicy.perSecond(1)));
        assertFalse(SimpleRefillPolicy.perSecond(5).equals(SimpleRefillPolicy.perSecond(6)));
    }
}
