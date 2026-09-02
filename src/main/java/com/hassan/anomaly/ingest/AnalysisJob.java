package com.hassan.anomaly.ingest;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "analysis_job",
    indexes = @Index(name = "idx_analysis_job_user", columnList = "username, created_at"))
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "upload_id", nullable = false)
    private String uploadId;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_bytes")
    private long totalBytes;

    @Column(name = "bytes_read")
    private long bytesRead;

    @Column(name = "rows_read")
    private long rowsRead;

    @Column(name = "rows_accepted")
    private long rowsAccepted;

    @Column(name = "rows_rejected")
    private long rowsRejected;

    @Column(name = "flagged_transactions")
    private int flaggedTransactions;

    @Column(name = "flagged_accounts")
    private int flaggedAccounts;

    @Column(name = "has_ground_truth", nullable = false)
    private boolean hasGroundTruth;

    @Column(name = "precision_score")
    private Double precision;

    @Column(name = "recall_score")
    private Double recall;

    @Lob
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Lob
    @Column(name = "errors_json", columnDefinition = "TEXT")
    private String errorsJson;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    protected AnalysisJob() {
    }

    public AnalysisJob(String username, String uploadId, String filename,
                       long totalBytes, boolean hasGroundTruth, String configJson) {
        this.username = username;
        this.uploadId = uploadId;
        this.filename = filename;
        this.totalBytes = totalBytes;
        this.hasGroundTruth = hasGroundTruth;
        this.configJson = configJson;
        this.status = JobStatus.QUEUED;
        this.createdAt = Instant.now();
    }

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void progress(long bytesRead, long rowsRead) {
        this.bytesRead = bytesRead;
        this.rowsRead = rowsRead;
    }

    public void markCompleted(long rowsRead, long rowsAccepted, long rowsRejected,
                              int flaggedTransactions, int flaggedAccounts,
                              Double precision, Double recall, String errorsJson) {
        this.status = JobStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.bytesRead = this.totalBytes;
        this.rowsRead = rowsRead;
        this.rowsAccepted = rowsAccepted;
        this.rowsRejected = rowsRejected;
        this.flaggedTransactions = flaggedTransactions;
        this.flaggedAccounts = flaggedAccounts;
        this.precision = precision;
        this.recall = recall;
        this.errorsJson = errorsJson;
    }

    public void markFailed(String reason) {
        this.status = JobStatus.FAILED;
        this.finishedAt = Instant.now();
        this.failureReason = reason == null ? "Unknown error"
                : reason.substring(0, Math.min(reason.length(), 500));
    }

    public int percentComplete() {
        if (status == JobStatus.COMPLETED) {
            return 100;
        }
        if (status == JobStatus.QUEUED || totalBytes <= 0) {
            return 0;
        }
        return (int) Math.min(99, (bytesRead * 100) / totalBytes);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getUploadId() { return uploadId; }
    public String getFilename() { return filename; }
    public JobStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public long getTotalBytes() { return totalBytes; }
    public long getBytesRead() { return bytesRead; }
    public long getRowsRead() { return rowsRead; }
    public long getRowsAccepted() { return rowsAccepted; }
    public long getRowsRejected() { return rowsRejected; }
    public int getFlaggedTransactions() { return flaggedTransactions; }
    public int getFlaggedAccounts() { return flaggedAccounts; }
    public boolean isHasGroundTruth() { return hasGroundTruth; }
    public Double getPrecision() { return precision; }
    public Double getRecall() { return recall; }
    public String getConfigJson() { return configJson; }
    public String getErrorsJson() { return errorsJson; }
    public String getFailureReason() { return failureReason; }
}