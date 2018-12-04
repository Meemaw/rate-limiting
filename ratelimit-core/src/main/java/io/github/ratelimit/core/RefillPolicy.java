package io.github.ratelimit.core;

import java.time.Duration;

public interface RefillPolicy {

    /**
     * @param timeElapsed time from last distribution
     * @return int number of tokens to distribute
     */
    default int distributeNewTokens(Duration timeElapsed) {
        return (int) Math.floor(timeElapsed.toNanos() * getCapacity() / getSamplingPeriod().toNanos());
    }

    /**
     * @return long time between two consecutive refills
     */
    default long getNanosBetweenRefills() {
        int capacity = getCapacity();
        if (capacity == 0) {
            return Long.MAX_VALUE;
        }
        return getSamplingPeriod().toNanos() / capacity;
    }

    /**
     * @return int maximum capacity of the policy
     */
    int getCapacity();

    /**
     * @return SamplingPeriod samplingPeriod of the policy
     */
    Duration getSamplingPeriod();

}
