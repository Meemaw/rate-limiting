package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.RateLimitExceptions;
import io.github.ratelimit.core.RateLimitRecord;
import io.github.ratelimit.core.RefillPolicy;

import java.io.Externalizable;

public abstract class AbstractRecord implements RateLimitRecord, Externalizable {

    protected abstract long getNanosToConsumptionImpl(RefillPolicy policy, long missingTokens);

    protected boolean checkConsumptionLimits(RefillPolicy policy, long missingTokens, boolean throwOnPolicyExceds) {
        long tokenCount = getTokenCount();
        long toConsume = missingTokens + tokenCount;
        if (tokenCount >= toConsume) {
            return true;
        }

        int policyCapacity = policy.getCapacity();
        if (throwOnPolicyExceds && policyCapacity < toConsume) {
            throw RateLimitExceptions.tokenConsumptionsExcedsPolicy(toConsume, policyCapacity);
        }
        return false;
    }

    @Override
    public long getNanosToConsumption(RefillPolicy policy, long missingTokens, boolean throwOnPolicyExceeds) {
        if (checkConsumptionLimits(policy, missingTokens, throwOnPolicyExceeds)) {
            return 0;
        }
        return getNanosToConsumptionImpl(policy, missingTokens);
    }

}
