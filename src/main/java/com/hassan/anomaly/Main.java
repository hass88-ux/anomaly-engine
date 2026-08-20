package com.hassan.anomaly;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Transaction> all = SampleData.transactions();
        Rule rule = new VelocityRule(3, Duration.ofMinutes(10));

        List<Transaction> seen = new ArrayList<>();
        for (Transaction txn : all) {
            if (rule.isSuspicious(txn, seen)) {
                System.out.println("ALERT  " + txn.id() + "  " + rule.name());
            }
            seen.add(txn);
        }
        System.out.println("Processed " + all.size() + " transactions");
    }
}