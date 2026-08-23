package com.hassan.anomaly;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Superseded by SpendVelocityRule, which counts spend rather than transactions.
 * Retained so the comparison documented in the README remains verifiable:
 * this rule fired on 51 legitimate transactions where SpendVelocity fired on 1.
 */
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
    public boolean isSuspicious(TransactionView txn, AccountHistory history) {
        Instant cutoff = txn.occurredAt().minus(window);
        List<TransactionView> recent = history.since(txn.accountId(), cutoff);
        return recent.size() >= maxCount;
    }
}