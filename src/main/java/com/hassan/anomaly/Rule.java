package com.hassan.anomaly;

public interface Rule {
    String name();
    boolean isSuspicious(TransactionView txn, AccountHistory history);
}