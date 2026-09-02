package com.hassan.anomaly.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class JobController {

    private static final int MAX_ALERTS_RETURNED = 200;

    private final UploadStore store;
    private final AnalysisJobRepository jobs;
    private final JobAlertRepository jobAlerts;
    private final UploadAnalysisService analysis;
    private final ObjectMapper objectMapper;

    public JobController(UploadStore store,
                         AnalysisJobRepository jobs,
                         JobAlertRepository jobAlerts,
                         UploadAnalysisService analysis,
                         ObjectMapper objectMapper) {
        this.store = store;
        this.jobs = jobs;
        this.jobAlerts = jobAlerts;
        this.analysis = analysis;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/uploads/{uploadId}/analyze")
    public ResponseEntity<?> analyze(
            @PathVariable String uploadId,
            @Valid @RequestBody AnalysisRequest request,
            @AuthenticationPrincipal String username) {

        if (!request.mapping().hasRequired()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "The account, timestamp and amount columns must all be mapped"));
        }

        if (!store.exists(uploadId, username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "That upload has expired or does not exist. Please upload again."));
        }

        long totalBytes;
        try {
            totalBytes = store.sizeOf(uploadId, username);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Could not read the uploaded file"));
        }

        String configJson;
        try {
            configJson = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            configJson = "{}";
        }

        AnalysisJob job = jobs.save(new AnalysisJob(
                username, uploadId, uploadId + ".csv",
                totalBytes, request.mapping().hasGroundTruth(), configJson));

        try {
            analysis.runAsync(job.getId(), username, request);
        } catch (RejectedExecutionException e) {
            job.markFailed("The server is busy. Please try again shortly.");
            jobs.save(job);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "30")
                    .body(Map.of("error", "Too many analyses running. Try again in a moment."));
        }

        return ResponseEntity.accepted().body(Map.of("jobId", job.getId()));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<JobView> job(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {

        return jobs.findByIdAndUsername(id, username)
                .map(job -> ResponseEntity.ok(toView(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs")
    public List<JobView> recent(@AuthenticationPrincipal String username) {
        return jobs.findAllByUsernameOrderByCreatedAtDesc(
                        username, PageRequest.of(0, 20, Sort.unsorted()))
                .stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/jobs/{id}/alerts")
    public ResponseEntity<?> alerts(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {

        if (jobs.findByIdAndUsername(id, username).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<JobAlert> stored = jobAlerts.findAllByJobId(
                id, PageRequest.of(0, MAX_ALERTS_RETURNED, Sort.unsorted()));

        List<JsonNode> accountAlerts = new ArrayList<>();
        for (JobAlert alert : stored) {
            try {
                accountAlerts.add(objectMapper.readTree(alert.getDetailJson()));
            } catch (Exception ignored) {
                // a single unreadable blob should not fail the whole page
            }
        }

        return ResponseEntity.ok(Map.of(
                "accountAlerts", accountAlerts,
                "totalFlaggedAccounts", jobAlerts.countByJobId(id)));
    }

    private JobView toView(AnalysisJob job) {
        List<ParseError> errors = List.of();
        if (job.getErrorsJson() != null && !job.getErrorsJson().isBlank()) {
            try {
                errors = objectMapper.readValue(
                        job.getErrorsJson(), new TypeReference<List<ParseError>>() {});
            } catch (Exception ignored) {
                // fall through to an empty list
            }
        }

        return new JobView(
                job.getId(),
                job.getFilename(),
                job.getStatus(),
                job.percentComplete(),
                job.getCreatedAt(),
                job.getFinishedAt(),
                job.getRowsRead(),
                job.getRowsAccepted(),
                job.getRowsRejected(),
                job.getFlaggedTransactions(),
                job.getFlaggedAccounts(),
                job.isHasGroundTruth(),
                job.getPrecision(),
                job.getRecall(),
                errors,
                job.getRowsRejected() > errors.size(),
                job.getFailureReason());
    }
}