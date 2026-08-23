package com.hassan.anomaly;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(
        String id,
        String accountId,
        Instant occurredAt,
        BigDecimal amount,
        String country,
        double latitude,
        double longitude
) {
    public static TransactionView of(Transaction txn) {
        return new TransactionView(txn.id(), txn.accountId(),
                txn.occurredAt(), txn.amount(), txn.country(),
                txn.latitude(), txn.longitude());
    }
}