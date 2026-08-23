package com.hassan.anomaly;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String id,
        String accountId,
        Instant occurredAt,
        BigDecimal amount,
        String country,
        double latitude,
        double longitude,
        boolean isFraud
) {}