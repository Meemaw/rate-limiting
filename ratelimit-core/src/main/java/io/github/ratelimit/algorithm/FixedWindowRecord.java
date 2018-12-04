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

public class FixedWindowRecord extends AbstractTokenRecord {
    private static final long serialVersionUID = 2474599140620682368L;

    private Instant windowEnd;

    public FixedWindowRecord() {
        super();
    }

    private FixedWindowRecord(long tokenCount, Instant windowEnd) {
        super(tokenCount);
        this.windowEnd = Objects.requireNonNull(windowEnd);
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public static FixedWindowRecord empty() {
        return FixedWindowRecord.from(0L);
    }

    public static FixedWindowRecord from(long tokenCount) {
        return FixedWindowRecord.of(tokenCount, Instant.now());
    }

    public static FixedWindowRecord of(long tokenCount, Instant windowEnd) {
        return new FixedWindowRecord(tokenCount, windowEnd);
    }

    @Override
    public RateLimitRecord updateWith(RefillPolicy policy, Instant requestTs) {
        if (requestTs.isAfter(windowEnd)) {
            return new FixedWindowRecord(policy.getCapacity(), windowEnd.plus(policy.getSamplingPeriod()));
        }
        return this;
    }

    @Override
    public RateLimitRecord consume(long numTokens) {
        return new FixedWindowRecord(tokenCount - numTokens, windowEnd);
    }

    @Override
    protected long getNanosToConsumptionImpl(RefillPolicy policy, long tokenConsumption) {
        return Duration.between(Instant.now(), windowEnd).toNanos();
    }

    @Override
    public String toString() {
        return String.format("%s = {tokenCount = %d, windowEnd = %s}", this.getClass().getSimpleName(), tokenCount,
                windowEnd);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(tokenCount);
        out.writeLong(windowEnd.toEpochMilli());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        tokenCount = in.readLong();
        windowEnd = Instant.ofEpochMilli(in.readLong());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FixedWindowRecord)) return false;
        if (!super.equals(o)) return false;

        FixedWindowRecord that = (FixedWindowRecord) o;

        if (tokenCount != that.tokenCount) return false;
        return windowEnd.equals(that.windowEnd);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + windowEnd.hashCode();
        return result;
    }
}
