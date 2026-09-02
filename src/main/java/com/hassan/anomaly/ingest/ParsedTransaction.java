package com.hassan.anomaly.ingest;

import java.math.BigDecimal;
import java.time.Instant;

public record ParsedTransaction(
        String transactionId,
        String accountId,
        Instant occurredAt,
        BigDecimal amount,
        Double latitude,
        Double longitude,
        Boolean isFraud
) {
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
}