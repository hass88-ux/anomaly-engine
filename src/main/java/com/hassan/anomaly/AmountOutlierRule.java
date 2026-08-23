package com.hassan.anomaly;

import java.util.List;

public class AmountOutlierRule implements Rule {

    private final double multiplier;
    private final int minimumHistory;

    public AmountOutlierRule(double multiplier, int minimumHistory) {
        this.multiplier = multiplier;
        this.minimumHistory = minimumHistory;
    }

    @Override
    public String name() {
        return "AmountOutlier(x" + multiplier + ", min " + minimumHistory + ")";
    }

    @Override
    public boolean isSuspicious(TransactionView txn, AccountHistory history) {
        List<TransactionView> sameAccount = history.forAccount(txn.accountId());

        if (sameAccount.size() < minimumHistory) {
            return false;
        }

        double mean = sameAccount.stream()
                .mapToDouble(t -> t.amount().doubleValue())
                .average()
                .orElse(0);

        if (mean == 0) {
            return false;
        }

        double value = txn.amount().doubleValue();
        return value > mean * multiplier || value < mean / multiplier;
    }
}