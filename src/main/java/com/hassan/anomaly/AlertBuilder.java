package com.hassan.anomaly;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AlertBuilder {

    private AlertBuilder() {}

    public static List<AccountAlert> groupByAccount(List<AlertRecord> alerts, int limit) {
        Map<String, List<AlertRecord>> byAccount = alerts.stream()
                .collect(Collectors.groupingBy(AlertRecord::accountId));

        List<AccountAlert> out = new ArrayList<>();

        for (Map.Entry<String, List<AlertRecord>> entry : byAccount.entrySet()) {
            List<AlertRecord> txns = entry.getValue();

            Set<String> rules = new LinkedHashSet<>();
            BigDecimal total = BigDecimal.ZERO;
            boolean anyFraud = false;

            for (AlertRecord a : txns) {
                rules.addAll(a.firedRules());
                total = total.add(a.amount());
                anyFraud = anyFraud || a.actuallyFraud();
            }

            out.add(new AccountAlert(
                    entry.getKey(),
                    confidence(txns.size(), rules.size()),
                    txns.size(),
                    rules.size(),
                    total,
                    List.copyOf(rules),
                    txns,
                    anyFraud));
        }

        out.sort(Comparator
                .comparingInt((AccountAlert a) -> rank(a.confidence())).reversed()
                .thenComparing(Comparator.comparingInt(AccountAlert::flaggedTransactions).reversed()));

        return out.size() > limit ? out.subList(0, limit) : out;
    }

    private static String confidence(int flaggedCount, int distinctRules) {
        if (distinctRules >= 2 || flaggedCount >= 3) {
            return "HIGH";
        }
        if (flaggedCount == 2) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static int rank(String confidence) {
        return switch (confidence) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }
}