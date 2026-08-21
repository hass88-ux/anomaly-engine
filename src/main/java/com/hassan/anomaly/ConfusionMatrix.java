package com.hassan.anomaly;

public class ConfusionMatrix {

    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;

    public void record(boolean flagged, boolean actuallyFraud) {
        if (flagged && actuallyFraud)        truePositives++;
        else if (flagged && !actuallyFraud)  falsePositives++;
        else if (!flagged && actuallyFraud)  falseNegatives++;
        else                                 trueNegatives++;
    }

    public double precision() {
        int flagged = truePositives + falsePositives;
        return flagged == 0 ? 0.0 : (double) truePositives / flagged;
    }

    public double recall() {
        int actual = truePositives + falseNegatives;
        return actual == 0 ? 0.0 : (double) truePositives / actual;
    }

    public void print() {
        System.out.println();
        System.out.println("TP " + truePositives + "   FP " + falsePositives
                + "   FN " + falseNegatives + "   TN " + trueNegatives);
        System.out.printf("Precision %.2f   Recall %.2f%n", precision(), recall());
    }
}