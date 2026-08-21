package com.hassan.anomaly;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class VelocityRule implements Rule {

    private final int maxCount;
    private final Duration window;

    public VelocityRule(int maxCount, Duration window) {
        this.maxCount = maxCount;
        this.window = window;
    }

    @Override
    public String name() {
        return "Velocity(" + maxCount + " in " + window.toMinutes() + "min)";
    }

    @Override
    public boolean isSuspicious(TransactionView txn, List<TransactionView> history) {
        Instant cutoff = txn.occurredAt().minus(window);
        long recent = history.stream()
                .filter(t -> t.accountId().equals(txn.accountId()))
                .filter(t -> t.occurredAt().isAfter(cutoff))
                .count();
        return recent >= maxCount;
    }
}