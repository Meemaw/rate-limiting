package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimitExceptions;
import io.github.ratelimit.core.RateLimitRecord;
import io.github.ratelimit.core.RefillPolicy;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;
import java.util.Objects;

public abstract class AbstractEntry implements RateLimitEntry {

    protected RateLimitRecord record;
    protected RefillPolicy policy;
    protected boolean throwOnPolicyExceds;

    protected AbstractEntry() {
    }

    protected AbstractEntry(RateLimitRecord record, RefillPolicy policy) {
        this(record, policy, false);
    }

    protected AbstractEntry(RateLimitRecord record, RefillPolicy policy, boolean throwOnPolicyExceeds) {
        this.record = Objects.requireNonNull(record);
        this.policy = Objects.requireNonNull(policy);
        this.throwOnPolicyExceds = throwOnPolicyExceeds;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(throwOnPolicyExceds);
        out.writeObject(record);
        out.writeObject(policy);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.throwOnPolicyExceds = in.readBoolean();
        this.record = (RateLimitRecord) in.readObject();
        this.policy = (RefillPolicy) in.readObject();
    }

    public abstract boolean tryConsume(long numTokens);

    protected boolean tryConsumeTokens(long numTokens) {
        return tryConsumeTokens(numTokens, Instant.now());
    }

    protected boolean tryConsumeTokens(long numTokens, Instant ts) {
        if (numTokens <= 0) {
            throw RateLimitExceptions.nonPositiveNumber(numTokens);
        }
        record = record.updateWith(policy, ts);
        if (!record.canConsume(numTokens)) {
            return false;
        }
        record = record.consume(numTokens);
        return true;
    }

    private ConsumptionEntry consumptionEntryFor(boolean canConsume, long numTokens) {
        if (canConsume) {
            return ConsumptionEntry.conformant(getTokenCount());
        } else {
            long missingTokens = numTokens - getTokenCount();
            return ConsumptionEntry.rejected(record.getTokenCount(), getNanosToConsumption(missingTokens), getRefillPolicy());
        }
    }

    @Override
    public ConsumptionEntry tryConsumeAndReturnRemaining(long numTokens) {
        return consumptionEntryFor(tryConsumeTokens(numTokens), numTokens);
    }

    @Override
    public ConsumptionEntry canConsumeAndReturnRemaining(long toConsume, Instant ts) {
        long tokenCount = getTokenCount();
        if (!record.canConsume(toConsume)) {
            long missingTokens = toConsume - tokenCount;
            return ConsumptionEntry.rejected(record.getTokenCount(), getNanosToConsumption(missingTokens), getRefillPolicy());
        }
        return ConsumptionEntry.conformant(tokenCount - toConsume);
    }

    @Override
    public long getNanosToConsumption(long consumptionCount) {
        return record.getNanosToConsumption(policy, consumptionCount, throwOnPolicyExceds);
    }

    @Override
    public RefillPolicy getRefillPolicy() {
        return policy;
    }

    ;

    @Override
    public RateLimitRecord getRateLimitRecord() {
        return record;
    }

    @Override
    public RateLimitEntry updateInPlace(Instant timestamp) {
        record = record.updateWith(policy, timestamp);
        return this;
    }

    @Override
    public RateLimitEntry consumeInPlace(long numTokens) {
        record = record.consume(numTokens);
        return this;
    }

    @Override
    public String toString() {
        return String.format("RateLimitEntry{record=%s, policy=%s}", record.toString(), policy.toString());
    }

}
