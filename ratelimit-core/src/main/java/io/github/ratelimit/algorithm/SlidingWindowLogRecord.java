package io.github.ratelimit.algorithm;

import io.github.ratelimit.core.RateLimitRecord;
import io.github.ratelimit.core.RefillPolicy;
import io.github.ratelimit.core.impl.AbstractRecord;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SlidingWindowLogRecord extends AbstractRecord {
    private static final long serialVersionUID = -6619395629072636732L;

    private Map<Instant, Long> requestMap;
    private long capacity;
    private long requestsWeight;

    public SlidingWindowLogRecord() {
        super();
    }

    private SlidingWindowLogRecord(Map<Instant, Long> requestMap, long capacity) {
        this.requestMap = requestMap;
        this.capacity = capacity;
        this.requestsWeight = calculateRequestsWeight();
    }

    public static SlidingWindowLogRecord of(long numTokens, long capacity) {
        long requestsToFake = capacity - numTokens;
        return withRequestMap(createRequestMap(requestsToFake), capacity);
    }

    private long calculateRequestsWeight() {
        return requestMap.entrySet().stream().map(Map.Entry::getValue).reduce(0L, (a, b) -> a + b);
    }

    public Map<Instant, Long> getRequestMap() {
        return requestMap;
    }

    public long getCapacity() {
        return capacity;
    }

    public static SlidingWindowLogRecord withRequestMap(Map<Instant, Long> requestMap, long capacity) {
        return new SlidingWindowLogRecord(requestMap, capacity);
    }

    @Override
    public RateLimitRecord updateWith(RefillPolicy policy, Instant requestTs) {
        Instant slidingWindowStart = requestTs.minus(policy.getSamplingPeriod());
        requestMap.entrySet().removeIf(entry -> entry.getKey().isBefore(slidingWindowStart));
        requestsWeight = calculateRequestsWeight();
        return this;
    }

    @Override
    public long getTokenCount() {
        return capacity - requestsWeight;
    }

    @Override
    public RateLimitRecord consume(long numTokens) {
        Instant now = Instant.now();
        requestMap.put(now, requestMap.getOrDefault(now, 0L) + numTokens);
        requestsWeight = calculateRequestsWeight();
        return this;
    }

    @Override
    public boolean canConsume(long numTokens) {
        return getTokenCount() >= numTokens;
    }

    private static Map<Instant, Long> createRequestMap(long numRequests) {
        Map<Instant, Long> requstMap = new LinkedHashMap<>();
        if (numRequests > 0L) {
            requstMap.put(Instant.now(), numRequests);
        }
        return requstMap;
    }

    @Override
    protected long getNanosToConsumptionImpl(RefillPolicy policy, long missingTokens) {
        Instant slidingWindowStart = Instant.now().minus(policy.getSamplingPeriod());
        Iterator<Map.Entry<Instant, Long>> it = requestMap.entrySet().iterator();

        long tokensAccomulated = 0L;
        while (it.hasNext()) {
            Map.Entry<Instant, Long> entry = it.next();
            tokensAccomulated += entry.getValue();

            if (tokensAccomulated >= missingTokens) {
                return Duration.between(slidingWindowStart, entry.getKey()).toNanos();
            }
        }

        return Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return String.format("%s = {tokenCount: %d, requestMap = %s}", this.getClass().getSimpleName(), getTokenCount(),
                requestMap);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(requestMap.size());
        out.writeLong(capacity);
        for (Map.Entry<Instant, Long> entry : requestMap.entrySet()) {
            out.writeLong(entry.getKey().toEpochMilli());
            out.writeLong(entry.getValue());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int mapSize = in.readInt();
        capacity = in.readLong();
        requestMap = new LinkedHashMap<>();
        for (int i = 0; i < mapSize; i++) {
            requestMap.put(Instant.ofEpochMilli(in.readLong()), in.readLong());
        }
    }

}
