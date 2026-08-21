package com.hassan.anomaly;

import java.util.List;

public interface Rule {
    String name();
    boolean isSuspicious(TransactionView txn, List<TransactionView> history);
}