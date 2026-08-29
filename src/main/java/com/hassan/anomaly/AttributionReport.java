package com.hassan.anomaly;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AttributionReport {

    private final Map<String, Integer> firedOnFraud = new LinkedHashMap<>();
    private final Map<String, Integer> firedOnLegit = new LinkedHashMap<>();
    private final Map<Integer, Integer> overlapHistogram = new LinkedHashMap<>();
    private final Map<String, Integer> comboCounts = new LinkedHashMap<>();

    private final Map<String, Integer> patternTotal = new TreeMap<>();
    private final Map<String, Integer> patternCaught = new TreeMap<>();
    private final Map<String, Map<String, Integer>> patternByRule = new TreeMap<>();

    public void record(List<String> firingRules, boolean actuallyFraud, String pattern) {
        for (String rule : firingRules) {
            Map<String, Integer> target = actuallyFraud ? firedOnFraud : firedOnLegit;
            target.merge(rule, 1, Integer::sum);
        }
        if (!firingRules.isEmpty() && actuallyFraud) {
            overlapHistogram.merge(firingRules.size(), 1, Integer::sum);
        }
        if (firingRules.size() > 1 && actuallyFraud) {
            comboCounts.merge(String.join(" + ", firingRules), 1, Integer::sum);
        }

        patternTotal.merge(pattern, 1, Integer::sum);
        if (!firingRules.isEmpty()) {
            patternCaught.merge(pattern, 1, Integer::sum);
            Map<String, Integer> byRule =
                    patternByRule.computeIfAbsent(pattern, k -> new LinkedHashMap<>());
            for (String rule : firingRules) {
                byRule.merge(rule, 1, Integer::sum);
            }
        }
    }

    public Set<String> allRules() {
        Set<String> rules = new LinkedHashSet<>(firedOnFraud.keySet());
        rules.addAll(firedOnLegit.keySet());
        return rules;
    }

    public int firedOnFraud(String rule) {
        return firedOnFraud.getOrDefault(rule, 0);
    }

    public int firedOnLegit(String rule) {
        return firedOnLegit.getOrDefault(rule, 0);
    }

    public Set<String> allPatterns() {
        return patternTotal.keySet();
    }

    public int patternTotal(String pattern) {
        return patternTotal.getOrDefault(pattern, 0);
    }

    public int patternCaught(String pattern) {
        return patternCaught.getOrDefault(pattern, 0);
    }

    public Map<String, Integer> patternByRule(String pattern) {
        return patternByRule.getOrDefault(pattern, Map.of());
    }

    public void print() {
        System.out.println();
        System.out.println("Per-rule firing counts (on fraud / on legitimate)");
        for (String rule : allRules()) {
            System.out.printf("  %-32s %4d / %d%n", rule,
                    firedOnFraud(rule), firedOnLegit(rule));
        }

        System.out.println();
        System.out.println("Caught fraud by number of rules firing");
        overlapHistogram.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %d rule(s): %d transactions%n",
                        e.getKey(), e.getValue()));

        System.out.println();
        System.out.println("Multi-rule combinations on fraud");
        comboCounts.forEach((combo, count) ->
                System.out.printf("  %-60s %d%n", combo, count));

        System.out.println();
        System.out.println("Per-pattern detection rate");
        for (String pattern : allPatterns()) {
            int total = patternTotal(pattern);
            int caught = patternCaught(pattern);
            System.out.printf("  %-20s %4d/%-6d %.2f%n",
                    pattern, caught, total, (double) caught / total);
            patternByRule(pattern).forEach((rule, count) ->
                    System.out.printf("      %-40s %d%n", rule, count));
        }
    }
}