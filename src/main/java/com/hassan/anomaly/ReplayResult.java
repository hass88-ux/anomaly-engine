package com.hassan.anomaly;

import java.util.List;
import java.util.Map;

public record ReplayResult(
        int transactions,
        int fraudCount,
        long replayTimeMs,
        int truePositives,
        int falsePositives,
        int trueNegatives,
        int falseNegatives,
        double precision,
        double recall,
        List<RuleStat> ruleStats,
        List<PatternStat> patternStats
) {
    public record RuleStat(String rule, int firedOnFraud, int firedOnLegitimate) {}

    public record PatternStat(String pattern, int caught, int total,
                              double rate, Map<String, Integer> byRule) {}
}