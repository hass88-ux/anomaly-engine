package com.hassan.anomaly;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        int accounts = 3200;

        List<Transaction> all = new DataGenerator(42).generate(accounts, 30);

        List<Rule> rules = List.of(
            new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0),
            new AmountOutlierRule(4.0, 5),
            new GeoImpossibilityRule(900)
        );

        ConfusionMatrix matrix = new ConfusionMatrix();
        AttributionReport attribution = new AttributionReport();
        AccountHistory seen = new AccountHistory();

        long startNanos = System.nanoTime();

        for (Transaction txn : all) {
            TransactionView view = TransactionView.of(txn);

            List<String> fired = new ArrayList<>();
            for (Rule rule : rules) {
                if (rule.isSuspicious(view, seen)) {
                    fired.add(rule.name());
                }
            }

            matrix.record(!fired.isEmpty(), txn.isFraud());
            attribution.record(fired, txn.isFraud(), txn.fraudPattern());
            seen.add(view);
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        long fraudCount = all.stream().filter(Transaction::isFraud).count();
        System.out.println("Accounts: " + accounts);
        System.out.println("Transactions: " + all.size());
        System.out.printf("Fraud: %d (%.2f%%)%n",
                fraudCount, 100.0 * fraudCount / all.size());
        System.out.println("Replay time: " + elapsedMs + " ms");
        matrix.print();
        attribution.print();
    }
}