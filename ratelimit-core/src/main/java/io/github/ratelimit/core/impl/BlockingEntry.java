package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.BlockingStrategy;
import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimitRecord;
import io.github.ratelimit.core.RefillPolicy;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;

public class BlockingEntry extends AbstractEntry {

    private BlockingStrategy blockingStrategy;

    public BlockingEntry() {
        super();
    }

    public BlockingEntry(RateLimitRecord record, RefillPolicy policy, BlockingStrategy blockingStrategy) {
        this(record, policy, blockingStrategy, true);
    }

    public BlockingEntry(RateLimitRecord record, RefillPolicy policy, BlockingStrategy blockingStrategy, boolean throwOnLimitExceed) {
        super(record, policy, throwOnLimitExceed);
        this.blockingStrategy = blockingStrategy;
    }

    @Override
    public boolean tryConsume(long numTokens, Instant ts) {
        boolean canConsume = tryConsumeTokens(numTokens, ts);
        if (canConsume) {
            return true;
        }
        long missingTokens = numTokens - getTokenCount();
        long nanosToPark = getNanosToConsumption(missingTokens);
        try {
            blockingStrategy.block(nanosToPark);
            return tryConsumeTokens(numTokens);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public RateLimitEntry update(Instant timestamp) {
        return new BlockingEntry(record.updateWith(policy, timestamp), policy, blockingStrategy);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public boolean tryConsume(long numTokens) {
        return tryConsume(numTokens, Instant.now());
    }
}
