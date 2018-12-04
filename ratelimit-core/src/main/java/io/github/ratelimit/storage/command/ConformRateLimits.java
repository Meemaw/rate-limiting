package io.github.ratelimit.storage.command;

import io.github.ratelimit.core.RateLimitEntry;
import io.github.ratelimit.core.impl.ConsumptionEntry;

import java.time.Instant;
import java.util.List;

public class ConformRateLimits implements EntryStorageCommand<ConsumptionEntry> {

    private static final long serialVersionUID = -4007020666680329399L;

    private final int requestWeight;

    public ConformRateLimits(int requestWeight) {
        this.requestWeight = requestWeight;
    }

    public ConformRateLimits() {
        this(1);
    }

    @Override
    public ConsumptionEntry execute(List<RateLimitEntry> entries, Instant currentTime) {
        ConsumptionEntry conformanceConsumption = ConsumptionEntry.conformant(Long.MAX_VALUE);

        for (RateLimitEntry entry : entries) {
            entry = entry.updateInPlace(currentTime);
            ConsumptionEntry consumptionEntry = entry.canConsumeAndReturnRemaining(requestWeight, currentTime);
            if (!consumptionEntry.doesConform()) {
                if (conformanceConsumption.doesConform()) {
                    conformanceConsumption = consumptionEntry;
                } else if (consumptionEntry.getNanosUntilConsumption() > conformanceConsumption
                        .getNanosUntilConsumption()) {
                    conformanceConsumption = consumptionEntry;
                }
            } else if (conformanceConsumption.doesConform()
                    && consumptionEntry.getRemainingTokens() < conformanceConsumption.getRemainingTokens()) {
                conformanceConsumption = consumptionEntry;
            }
        }

        if (conformanceConsumption.doesConform()) {
            for (RateLimitEntry entry : entries) {
                entry.consumeInPlace(requestWeight);
            }
        }

        return conformanceConsumption;
    }

    @Override
    public boolean doesMutate() {
        return true;
    }

}

