package com.hassan.anomaly;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) {
	    List<Transaction> all = SampleData.transactions();
	    Rule rule = new VelocityRule(3, Duration.ofMinutes(10));
	    ConfusionMatrix matrix = new ConfusionMatrix();

	    List<Transaction> seen = new ArrayList<>();
	    for (Transaction txn : all) {
	        boolean flagged = rule.isSuspicious(txn, seen);
	        if (flagged) {
	            System.out.println("ALERT  " + txn.id() + "  " + rule.name());
	        }
	        matrix.record(flagged, txn.isFraud());
	        seen.add(txn);
	        }
	    System.out.println("Processed " + all.size() + " transactions");
	    matrix.print();
	}
}