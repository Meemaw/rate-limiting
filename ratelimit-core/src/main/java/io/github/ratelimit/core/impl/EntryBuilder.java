package io.github.ratelimit.core.impl;

import io.github.ratelimit.algorithm.FixedWindowRecord;
import io.github.ratelimit.algorithm.SlidingWindowLogRecord;
import io.github.ratelimit.algorithm.TokenBucketRecord;
import io.github.ratelimit.core.*;

import java.time.Instant;
import java.util.Objects;

public class EntryBuilder {

    protected RefillPolicy refillPolicy = SimpleRefillPolicy.perSecond(1);
    protected int initialTokenCount = 0;
    protected BlockingStrategy blockingStrategy;
    protected RateLimitAlgorithm rateLimitAlgorithm = RateLimitAlgorithm.TOKEN_BUCKET;
    protected RateLimitRecord record;
    protected boolean throwOnPolicyExceeds = false;

    public EntryBuilder withThrowOnPolicyExceds() {
        this.throwOnPolicyExceeds = true;
        return this;
    }

    public EntryBuilder filled() {
        return withInitialTokenCount(Objects.requireNonNull(refillPolicy).getCapacity());
    }

    public EntryBuilder withRefillPolicy(RefillPolicy refillPolicy) {
        this.refillPolicy = Objects.requireNonNull(refillPolicy);
        return this;
    }

    public EntryBuilder withInitialTokenCount(int initialTokenCount) {
        this.initialTokenCount = initialTokenCount;
        this.record = null;
        return this;
    }

    public EntryBuilder withBlockingStrategy(BlockingStrategy blockingStrategy) {
        this.blockingStrategy = blockingStrategy;
        return this;
    }

    public EntryBuilder withAlgorithm(RateLimitAlgorithm algorithm) {
        this.rateLimitAlgorithm = Objects.requireNonNull(algorithm);
        return this;
    }

    public EntryBuilder withRecord(RateLimitRecord record) {
        this.record = record;
        return this;
    }

    protected RateLimitRecord buildRecord() {
        switch (rateLimitAlgorithm) {
            case SLIDING_WINDOW:
                return SlidingWindowLogRecord.of(initialTokenCount, refillPolicy.getCapacity());
            case TOKEN_BUCKET:
                return TokenBucketRecord.from(initialTokenCount);
            case FIXED_WINDOW:
                return FixedWindowRecord.of(initialTokenCount, Instant.now().plus(refillPolicy.getSamplingPeriod()));
            default:
                throw RateLimitExceptions.unsupportedTechnique();
        }
    }

    public RateLimitEntry build() {
        RateLimitRecord buildRecord = record == null ? buildRecord() : record;
        if (blockingStrategy != null) {
            return new BlockingEntry(buildRecord, refillPolicy, blockingStrategy, throwOnPolicyExceeds);
        } else {
            return new NonBlockingEntry(buildRecord, refillPolicy, throwOnPolicyExceeds);
        }
    }

}
