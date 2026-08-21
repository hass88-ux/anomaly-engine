package com.hassan.anomaly;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Transaction> all = new DataGenerator(42).generate(400, 30);
        Rule rule = new VelocityRule(3, Duration.ofMinutes(3));
        ConfusionMatrix matrix = new ConfusionMatrix();

        List<Transaction> seen = new ArrayList<>();
        for (Transaction txn : all) {
            boolean flagged = rule.isSuspicious(txn, seen);
            matrix.record(flagged, txn.isFraud());
            seen.add(txn);
        }

        long fraudCount = all.stream().filter(Transaction::isFraud).count();
        System.out.println("Rule: " + rule.name());
        System.out.println("Transactions: " + all.size());
        System.out.printf("Fraud: %d (%.2f%%)%n",
                fraudCount, 100.0 * fraudCount / all.size());
        matrix.print();
    }
}