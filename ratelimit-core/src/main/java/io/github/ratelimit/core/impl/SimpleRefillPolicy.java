package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.RefillPolicy;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.util.Objects;

public class SimpleRefillPolicy implements RefillPolicy, Externalizable {

    protected int capacity;
    protected Duration samplingPeriod;

    public SimpleRefillPolicy() {
    }

    public SimpleRefillPolicy(int capacity, Duration samplingPeriod) {
        this.capacity = capacity;
        this.samplingPeriod = Objects.requireNonNull(samplingPeriod);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public Duration getSamplingPeriod() {
        return samplingPeriod;
    }

    public static SimpleRefillPolicy banPolicy() {
        return new SimpleRefillPolicy(0, Duration.ofSeconds(1));
    }

    public static SimpleRefillPolicy withRefill(int limit, Duration samplingPeriod) {
        return new SimpleRefillPolicy(limit, samplingPeriod);
    }

    public static SimpleRefillPolicy perSecond(int limit) {
        return new SimpleRefillPolicy(limit, Duration.ofSeconds(1));
    }

    public static SimpleRefillPolicy perMinute(int limit) {
        return new SimpleRefillPolicy(limit, Duration.ofMinutes(1));
    }

    public static SimpleRefillPolicy perHour(int limit) {
        return new SimpleRefillPolicy(limit, Duration.ofHours(1));
    }

    public static SimpleRefillPolicy perDay(int limit) {
        return new SimpleRefillPolicy(limit, Duration.ofDays(1));
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(capacity);
        out.writeLong(samplingPeriod.toNanos());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        capacity = in.readInt();
        samplingPeriod = Duration.ofNanos(in.readLong());
    }

    @Override
    public String toString() {
        return String.format("{\"samplingPeriod\": \"%s\", \"limit\": %d}", samplingPeriod, capacity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleRefillPolicy)) return false;

        SimpleRefillPolicy that = (SimpleRefillPolicy) o;

        if (capacity != that.capacity) return false;
        return samplingPeriod.equals(that.samplingPeriod);
    }

    @Override
    public int hashCode() {
        int result = capacity;
        result = 31 * result + samplingPeriod.hashCode();
        return result;
    }
}
