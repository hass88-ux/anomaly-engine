package com.hassan.anomaly;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class SpendVelocityRule implements Rule {

    private final int minCount;
    private final Duration window;
    private final double spendMultiplier;

    public SpendVelocityRule(int minCount, Duration window, double spendMultiplier) {
        this.minCount = minCount;
        this.window = window;
        this.spendMultiplier = spendMultiplier;
    }

    @Override
    public String name() {
        return "SpendVelocity(" + minCount + " in " + window.toMinutes()
                + "min, x" + spendMultiplier + ")";
    }

    @Override
    public boolean isSuspicious(TransactionView txn, List<TransactionView> history) {
        List<TransactionView> sameAccount = history.stream()
                .filter(t -> t.accountId().equals(txn.accountId()))
                .toList();

        Instant cutoff = txn.occurredAt().minus(window);
        List<TransactionView> recent = sameAccount.stream()
                .filter(t -> t.occurredAt().isAfter(cutoff))
                .toList();

        if (recent.size() < minCount) {
            return false;
        }

        double accountMean = sameAccount.stream()
                .mapToDouble(t -> t.amount().doubleValue())
                .average()
                .orElse(0);

        if (accountMean <= 0) {
            return false;
        }

        double windowSpend = recent.stream()
                .mapToDouble(t -> t.amount().doubleValue())
                .sum() + txn.amount().doubleValue();

        return windowSpend > accountMean * spendMultiplier;
    }
}