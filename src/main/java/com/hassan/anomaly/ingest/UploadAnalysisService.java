package com.hassan.anomaly.ingest;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hassan.anomaly.AccountAlert;
import com.hassan.anomaly.AccountHistory;
import com.hassan.anomaly.AlertBuilder;
import com.hassan.anomaly.AlertRecord;
import com.hassan.anomaly.AmountOutlierRule;
import com.hassan.anomaly.Cities;
import com.hassan.anomaly.CityGazetteer;
import com.hassan.anomaly.ConfusionMatrix;
import com.hassan.anomaly.GeoImpossibilityRule;
import com.hassan.anomaly.Rule;
import com.hassan.anomaly.SpendVelocityRule;
import com.hassan.anomaly.Transaction;
import com.hassan.anomaly.TransactionView;

@Service
public class UploadAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(UploadAnalysisService.class);

    private static final int STORED_ALERT_LIMIT = 500;
    private static final long PROGRESS_EVERY_ROWS = 2000;

    private final UploadStore store;
    private final AnalysisJobRepository jobs;
    private final JobAlertRepository jobAlerts;
    private final CityGazetteer gazetteer;
    private final ObjectMapper objectMapper;
    private final long maxRows;

    public UploadAnalysisService(UploadStore store,
                                 AnalysisJobRepository jobs,
                                 JobAlertRepository jobAlerts,
                                 CityGazetteer gazetteer,
                                 ObjectMapper objectMapper,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${anomaly.upload.max-rows:500000}") long maxRows) {
        this.store = store;
        this.jobs = jobs;
        this.jobAlerts = jobAlerts;
        this.gazetteer = gazetteer;
        this.objectMapper = objectMapper;
        this.maxRows = maxRows;
    }

    @Async("analysisExecutor")
    public void runAsync(Long jobId, String username, AnalysisRequest request) {
        try {
            run(jobId, username, request);
        } catch (Exception e) {
            log.error("Analysis job {} failed", jobId, e);
            fail(jobId, e.getMessage());
        }
    }

    private void run(Long jobId, String username, AnalysisRequest request) throws IOException {
        AnalysisJob job = jobs.findByIdAndUsername(jobId, username)
                .orElseThrow(() -> new IllegalStateException("Job " + jobId + " not found"));

        job.markRunning();
        jobs.save(job);

        ColumnMapping mapping = request.mapping();

        List<Rule> rules = new ArrayList<>();
        rules.add(new SpendVelocityRule(
                request.velocityMinCount(),
                Duration.ofMinutes(request.velocityWindowMinutes()),
                request.velocitySpendMultiplier()));
        rules.add(new AmountOutlierRule(
                request.amountMultiplier(),
                request.amountMinHistory()));
        if (mapping.hasGeo()) {
            rules.add(new GeoImpossibilityRule(request.geoMaxSpeedKmh()));
        }

        ConfusionMatrix matrix = new ConfusionMatrix();
        AccountHistory seen = new AccountHistory();
        List<AlertRecord> alerts = new ArrayList<>();

        AtomicLong rowCounter = new AtomicLong();
        long totalBytes = store.sizeOf(job.getUploadId(), username);

        TransactionParser parser = new TransactionParser(mapping, maxRows);

        try (InputStream in = store.open(job.getUploadId(), username);
             CountingInputStream counting = new CountingInputStream(in)) {

            parser.parse(counting, parsed -> {
                Transaction txn = toTransaction(parsed);
                TransactionView view = TransactionView.of(txn);

                List<String> fired = new ArrayList<>();
                for (Rule rule : rules) {
                    if (rule.isSuspicious(view, seen)) {
                        fired.add(rule.name());
                    }
                }

                if (!fired.isEmpty()) {
                    Cities.City where = parsed.hasLocation()
                            ? gazetteer.nearest(parsed.latitude(), parsed.longitude())
                            : null;

                    alerts.add(new AlertRecord(
                            parsed.transactionId(), parsed.accountId(),
                            parsed.occurredAt(), parsed.amount(),
                            parsed.latitude() == null ? 0 : parsed.latitude(),
                            parsed.longitude() == null ? 0 : parsed.longitude(),
                            where == null ? null : where.name(),
                            where == null ? null : where.province(),
                            List.copyOf(fired),
                            Boolean.TRUE.equals(parsed.isFraud())));
                }

                if (parsed.isFraud() != null) {
                    matrix.record(!fired.isEmpty(), parsed.isFraud());
                }

                seen.add(view);

                long n = rowCounter.incrementAndGet();
                if (n % PROGRESS_EVERY_ROWS == 0) {
                    updateProgress(jobId, counting.count(), n);
                }
            });

            complete(jobId, username, parser, alerts, matrix,
                    mapping.hasGroundTruth(), totalBytes);
        }
    }

    private Transaction toTransaction(ParsedTransaction parsed) {
        return new Transaction(
                parsed.transactionId(),
                parsed.accountId(),
                parsed.occurredAt(),
                parsed.amount(),
                "CA",
                parsed.latitude() == null ? 0.0 : parsed.latitude(),
                parsed.longitude() == null ? 0.0 : parsed.longitude(),
                Boolean.TRUE.equals(parsed.isFraud()),
                "NONE");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgress(Long jobId, long bytesRead, long rowsRead) {
        jobs.findById(jobId).ifPresent(job -> {
            job.progress(bytesRead, rowsRead);
            jobs.save(job);
        });
    }

    @Transactional
    public void complete(Long jobId, String username, TransactionParser parser,
                         List<AlertRecord> alerts, ConfusionMatrix matrix,
                         boolean hasGroundTruth, long totalBytes) {

        List<AccountAlert> grouped = AlertBuilder.groupByAccount(alerts, STORED_ALERT_LIMIT);

        AnalysisJob job = jobs.findByIdAndUsername(jobId, username).orElseThrow();

        for (AccountAlert account : grouped) {
            try {
                jobAlerts.save(new JobAlert(
                        jobId,
                        account.accountId(),
                        account.confidence(),
                        account.flaggedTransactions(),
                        account.distinctRules(),
                        account.totalFlaggedAmount(),
                        hasGroundTruth ? account.anyActuallyFraud() : null,
                        objectMapper.writeValueAsString(account)));
            } catch (Exception e) {
                log.warn("Could not store alert for account {}", account.accountId(), e);
            }
        }

        int distinctAccounts = (int) alerts.stream()
                .map(AlertRecord::accountId)
                .distinct()
                .count();

        String errorsJson;
        try {
            errorsJson = objectMapper.writeValueAsString(parser.errors());
        } catch (Exception e) {
            errorsJson = "[]";
        }

        job.markCompleted(
                parser.rowsRead(), parser.rowsAccepted(), parser.rowsRejected(),
                alerts.size(), distinctAccounts,
                hasGroundTruth ? matrix.precision() : null,
                hasGroundTruth ? matrix.recall() : null,
                errorsJson);

        jobs.save(job);
        store.delete(job.getUploadId(), username);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long jobId, String reason) {
        jobs.findById(jobId).ifPresent(job -> {
            job.markFailed(reason);
            jobs.save(job);
        });
    }
}