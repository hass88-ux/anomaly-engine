package com.hassan.anomaly;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ReplayService {

    public ReplayResult run(ReplayRequest request) {
        List<Transaction> all = new DataGenerator(request.seed())
                .generate(request.accounts(), request.days());

        List<Rule> rules = List.of(
                new SpendVelocityRule(
                        request.velocityMinCount(),
                        Duration.ofMinutes(request.velocityWindowMinutes()),
                        request.velocitySpendMultiplier()),
                new AmountOutlierRule(
                        request.amountMultiplier(),
                        request.amountMinHistory()),
                new GeoImpossibilityRule(request.geoMaxSpeedKmh()));

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

        int fraudCount = (int) all.stream().filter(Transaction::isFraud).count();

        List<ReplayResult.RuleStat> ruleStats = attribution.allRules().stream()
                .map(r -> new ReplayResult.RuleStat(
                        r, attribution.firedOnFraud(r), attribution.firedOnLegit(r)))
                .toList();

        List<ReplayResult.PatternStat> patternStats = attribution.allPatterns().stream()
                .map(p -> {
                    int total = attribution.patternTotal(p);
                    int caught = attribution.patternCaught(p);
                    return new ReplayResult.PatternStat(p, caught, total,
                            total == 0 ? 0.0 : (double) caught / total,
                            attribution.patternByRule(p));
                })
                .toList();

        return new ReplayResult(
                all.size(), fraudCount, elapsedMs,
                matrix.truePositives(), matrix.falsePositives(),
                matrix.trueNegatives(), matrix.falseNegatives(),
                matrix.precision(), matrix.recall(),
                ruleStats, patternStats);
    }
}