package io.github.ratelimit.core;

import io.github.ratelimit.core.impl.SimpleRefillPolicy;

import java.time.Duration;

public final class RateLimitTestConstants {

    private RateLimitTestConstants() {
    }

    public static Duration DURATION_ONE_SECOND = Duration.ofSeconds(1);
    public static Duration DURATION_HALF_SECOND = Duration.ofMillis(500);
    public static RefillPolicy REFILL_FIVE_PER_SECOND = SimpleRefillPolicy.perSecond(5);
    public static RefillPolicy REFILL_TEN_PER_SECOND = SimpleRefillPolicy.perSecond(10);
}
