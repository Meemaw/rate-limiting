package io.github.ratelimit.algorithm;

import io.github.ratelimit.core.RateLimitRecord;
import io.github.ratelimit.core.RefillPolicy;
import io.github.ratelimit.core.impl.AbstractTokenRecord;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class TokenBucketRecord extends AbstractTokenRecord {

    private static final long serialVersionUID = 2854601848368994782L;

    protected Instant lastTokenDistributedTs;

    public TokenBucketRecord() {
        super();
    }

    public TokenBucketRecord(long tokenCount, Instant lastTokenDistributedTs) {
        super(tokenCount);
        this.lastTokenDistributedTs = Objects.requireNonNull(lastTokenDistributedTs);
    }

    public static TokenBucketRecord empty() {
        return TokenBucketRecord.from(0L);
    }

    public static TokenBucketRecord from(long tokenCount) {
        return TokenBucketRecord.of(tokenCount, Instant.now());
    }

    public static TokenBucketRecord of(long tokenCount, Instant timestamp) {
        return new TokenBucketRecord(tokenCount, timestamp);
    }

    @Override
    public RateLimitRecord updateWith(RefillPolicy policy, Instant requestTs) {
        Duration timeElapsed = Duration.between(lastTokenDistributedTs, requestTs);
        int numNewTokens = policy.distributeNewTokens(timeElapsed);
        if (numNewTokens > 0) {
            long newTokenCount = this.tokenCount + numNewTokens;
            long capacity = policy.getCapacity();
            if (newTokenCount > capacity) {
                newTokenCount = capacity;
            }
            return TokenBucketRecord.of(newTokenCount, requestTs);
        }
        return this;
    }

    @Override
    public RateLimitRecord consume(long numTokens) {
        return TokenBucketRecord.of(tokenCount - numTokens, lastTokenDistributedTs);
    }

    @Override
    public long getNanosToConsumptionImpl(RefillPolicy policy, long tokenConsumption) {
        long nanosBetweenRefills = policy.getNanosBetweenRefills();
        Instant nextRefill = lastTokenDistributedTs.plusNanos(nanosBetweenRefills);
        long nanosUntilNextToken = Duration.between(Instant.now(), nextRefill).toNanos();
        if (tokenConsumption == 1) {
            return nanosUntilNextToken;
        } else {
            return nanosUntilNextToken + ((tokenConsumption - 1) * nanosBetweenRefills);
        }
    }

    @Override
    public String toString() {
        return String.format("%s = {tokenCount: %d, lastTokenDistributedTs: %s}", this.getClass().getSimpleName(),
                tokenCount, lastTokenDistributedTs.toString());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(tokenCount);
        out.writeLong(lastTokenDistributedTs.toEpochMilli());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        tokenCount = in.readLong();
        lastTokenDistributedTs = Instant.ofEpochMilli(in.readLong());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenBucketRecord)) return false;
        if (!super.equals(o)) return false;

        TokenBucketRecord that = (TokenBucketRecord) o;

        if (tokenCount != that.tokenCount) return false;
        return lastTokenDistributedTs.equals(that.lastTokenDistributedTs);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + lastTokenDistributedTs.hashCode();
        return result;
    }
}
