package com.hassan.anomaly;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ReplayService {

    private static final int ALERT_LIMIT = 50;

    private final ReplayRunRepository repository;
    private final ObjectMapper objectMapper;

    public ReplayService(ReplayRunRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ReplayResult run(ReplayRequest request, String username) {
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
        List<AlertRecord> alerts = new ArrayList<>();

        long startNanos = System.nanoTime();

        for (Transaction txn : all) {
            TransactionView view = TransactionView.of(txn);

            List<String> fired = new ArrayList<>();
            for (Rule rule : rules) {
                if (rule.isSuspicious(view, seen)) {
                    fired.add(rule.name());
                }
            }

            if (!fired.isEmpty()) {
                alerts.add(new AlertRecord(
                        txn.id(), txn.accountId(), txn.occurredAt(), txn.amount(),
                        txn.latitude(), txn.longitude(),
                        List.copyOf(fired), txn.isFraud()));
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

        int distinctFlaggedAccounts = (int) alerts.stream()
                .map(AlertRecord::accountId)
                .distinct()
                .count();

        List<AccountAlert> accountAlerts = AlertBuilder.groupByAccount(alerts, ALERT_LIMIT);

        ReplayResult result = new ReplayResult(
                all.size(), fraudCount, elapsedMs,
                matrix.truePositives(), matrix.falsePositives(),
                matrix.trueNegatives(), matrix.falseNegatives(),
                matrix.precision(), matrix.recall(),
                ruleStats, patternStats,
                accountAlerts, distinctFlaggedAccounts);

        persist(request, result, username);

        return result;
    }

    private void persist(ReplayRequest request, ReplayResult result, String username) {
        try {
            String breakdown = objectMapper.writeValueAsString(
                    new Breakdown(result.ruleStats(), result.patternStats()));
            repository.save(new ReplayRun(Instant.now(), username, request, result, breakdown));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise replay breakdown", e);
        }
    }

    private record Breakdown(List<ReplayResult.RuleStat> ruleStats,
                             List<ReplayResult.PatternStat> patternStats) {}
}