package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.RateLimitRecord;
import io.github.ratelimit.core.RefillPolicy;

import java.time.Instant;

public class NonBlockingEntry extends AbstractEntry {

    private static final long serialVersionUID = 9149866964555052244L;

    public NonBlockingEntry() {
    }

    public NonBlockingEntry(RateLimitRecord record, RefillPolicy policy) {
        super(record, policy, false);
    }

    public NonBlockingEntry(RateLimitRecord record, RefillPolicy policy, boolean throwOnPolicyExceed) {
        super(record, policy, throwOnPolicyExceed);
    }

    @Override
    public boolean tryConsume(long numTokens) {
        return tryConsumeTokens(numTokens);
    }

    @Override
    public RateLimitEntry update(Instant timestamp) {
        return new NonBlockingEntry(record.updateWith(policy, timestamp), policy);
    }

    @Override
    public boolean tryConsume(long numTokens, Instant ts) {
        return tryConsumeTokens(numTokens, ts);
    }

}
