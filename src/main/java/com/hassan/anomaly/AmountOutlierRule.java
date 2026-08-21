package com.hassan.anomaly;

import java.math.BigDecimal;
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
    public boolean isSuspicious(TransactionView txn, List<TransactionView> history) {
        List<BigDecimal> amounts = history.stream()
                .filter(t -> t.accountId().equals(txn.accountId()))
                .map(TransactionView::amount)
                .toList();

        if (amounts.size() < minimumHistory) {
            return false;
        }

        double mean = amounts.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);

        if (mean == 0) {
            return false;
        }

        double value = txn.amount().doubleValue();
        return value > mean * multiplier || value < mean / multiplier;
    }
}