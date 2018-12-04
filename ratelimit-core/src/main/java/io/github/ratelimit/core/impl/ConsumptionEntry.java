package io.github.ratelimit.core.impl;

import io.github.ratelimit.core.RefillPolicy;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ConsumptionEntry implements Externalizable {

    private boolean conforms;
    private long remainingTokens;
    private long nanosUntilConsumption;
    private RefillPolicy violatedPolicy;

    public ConsumptionEntry() {
    }

    private ConsumptionEntry(boolean conforms, long remainingTokens, long nanosUntilConsumption,
                             RefillPolicy violatedPolicy) {
        this.conforms = conforms;
        this.remainingTokens = Math.max(0L, remainingTokens);
        this.nanosUntilConsumption = nanosUntilConsumption;
        this.violatedPolicy = violatedPolicy;
    }

    public static ConsumptionEntry conformant(long remainingTokens) {
        return new ConsumptionEntry(true, remainingTokens, 0, null);
    }

    public static ConsumptionEntry rejected(long remainingTokens, long nanosUntilConsumption,
                                            RefillPolicy violatedPolicy) {
        return new ConsumptionEntry(false, remainingTokens, nanosUntilConsumption, violatedPolicy);
    }

    public RefillPolicy getViolatedPolicy() {
        return violatedPolicy;
    }

    public boolean doesConform() {
        return conforms;
    }

    public long getNanosUntilConsumption() {
        return nanosUntilConsumption;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(conforms);
        out.writeLong(remainingTokens);
        out.writeLong(nanosUntilConsumption);
        if (!conforms) {
            out.writeObject(violatedPolicy);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        conforms = in.readBoolean();
        remainingTokens = in.readLong();
        nanosUntilConsumption = in.readLong();
        if (!conforms) {
            violatedPolicy = (RefillPolicy) in.readObject();
        }
    }

}
