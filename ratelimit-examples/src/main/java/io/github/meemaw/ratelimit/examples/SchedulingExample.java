package io.github.meemaw.ratelimit.examples;

import io.github.ratelimit.core.RateLimitAlgorithm;
import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimiting;
import io.github.ratelimit.core.RefillPolicy;
import io.github.ratelimit.core.impl.EntryBuilder;
import io.github.ratelimit.core.impl.SimpleRefillPolicy;

public class SchedulingExample {

    public static void main(String[] args) {
        EntryBuilder builder = RateLimiting.schedulerBuilder().withAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        RefillPolicy policy = SimpleRefillPolicy.perSecond(2);
        RateLimitEntry record = builder.withRefillPolicy(policy).build();

        long start = System.currentTimeMillis();
        while (record.tryConsume(1)) {
            double secondsPassed = (System.currentTimeMillis() - start) / 1000.0;
            System.out.println(secondsPassed); // or someVeryExpensiveTask();
        }
    }
}
