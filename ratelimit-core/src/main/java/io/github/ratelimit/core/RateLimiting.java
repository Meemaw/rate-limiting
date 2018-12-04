package io.github.ratelimit.core;

import io.github.ratelimit.core.impl.EntryBuilder;
import io.github.ratelimit.core.impl.RateLimiterImpl;

public final class RateLimiting {

    private RateLimiting() {
    }

    public static RateLimiter withStorage(EntryStorage storage) {
        return new RateLimiterImpl(storage);
    }

    public static EntryBuilder schedulerBuilder() {
        return new SchedulerBuilder();
    }

    public static EntryBuilder entryBuilder() {
        return new EntryBuilder();
    }

    public static class SchedulerBuilder extends EntryBuilder {

        public SchedulerBuilder() {
            this.blockingStrategy = BlockingStrategy.SLEEPING;
        }

        @Override
        public RateLimitEntry build() {
            if (blockingStrategy == null) {
                throw new IllegalStateException("BlockingStrategy should not be null for Scheduling!");
            }
            return super.build();
        }
    }

}
