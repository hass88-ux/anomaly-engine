package com.hassan.anomaly;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Transaction> all = new DataGenerator(42).generate(400, 30);

        List<Rule> rules = List.of(
        	    new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0),
        	    new AmountOutlierRule(4.0, 5),
        	    new GeoImpossibilityRule(900)
        	);

        ConfusionMatrix matrix = new ConfusionMatrix();
        AttributionReport attribution = new AttributionReport();
        List<TransactionView> seen = new ArrayList<>();

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

        long fraudCount = all.stream().filter(Transaction::isFraud).count();
        System.out.println("Rules: " + rules.size());
        rules.forEach(r -> System.out.println("  " + r.name()));
        System.out.println("Transactions: " + all.size());
        System.out.printf("Fraud: %d (%.2f%%)%n",
                fraudCount, 100.0 * fraudCount / all.size());
        matrix.print();
        attribution.print();
    }
}