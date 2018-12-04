package io.github.ratelimit.core.impl;

public abstract class AbstractTokenRecord extends AbstractRecord {

    protected long tokenCount;

    public AbstractTokenRecord() {
    }

    public AbstractTokenRecord(long tokenCount) {
        this.tokenCount = tokenCount;
    }

    @Override
    public long getTokenCount() {
        return tokenCount;
    }

    @Override
    public boolean canConsume(long numTokens) {
        return tokenCount >= numTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractTokenRecord)) return false;

        AbstractTokenRecord that = (AbstractTokenRecord) o;

        return tokenCount == that.tokenCount;
    }

    @Override
    public int hashCode() {
        return (int) (tokenCount ^ (tokenCount >>> 32));
    }
}
