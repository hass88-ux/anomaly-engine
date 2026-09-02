package com.hassan.anomaly;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AlertRecord(
        String transactionId,
        String accountId,
        Instant occurredAt,
        BigDecimal amount,
        double latitude,
        double longitude,
        String city,
        String province,
        List<String> firedRules,
        boolean actuallyFraud
) {}