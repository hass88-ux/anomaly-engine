package com.hassan.anomaly;

import java.math.BigDecimal;
import java.util.List;

public record AccountAlert(
        String accountId,
        String confidence,
        int flaggedTransactions,
        int distinctRules,
        BigDecimal totalFlaggedAmount,
        List<String> rulesTriggered,
        List<AlertRecord> transactions,
        boolean anyActuallyFraud
) {}