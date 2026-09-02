package com.hassan.anomaly.ingest;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "job_alert",
    indexes = @Index(name = "idx_job_alert_job", columnList = "job_id"))
public class JobAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false, length = 10)
    private String confidence;

    @Column(name = "flagged_transactions", nullable = false)
    private int flaggedTransactions;

    @Column(name = "distinct_rules", nullable = false)
    private int distinctRules;

    @Column(name = "total_flagged_amount", precision = 19, scale = 2)
    private BigDecimal totalFlaggedAmount;

    @Column(name = "any_actually_fraud")
    private Boolean anyActuallyFraud;

    @Lob
    @Column(name = "detail_json", columnDefinition = "TEXT", nullable = false)
    private String detailJson;

    protected JobAlert() {
    }

    public JobAlert(Long jobId, String accountId, String confidence,
                    int flaggedTransactions, int distinctRules,
                    BigDecimal totalFlaggedAmount, Boolean anyActuallyFraud,
                    String detailJson) {
        this.jobId = jobId;
        this.accountId = accountId;
        this.confidence = confidence;
        this.flaggedTransactions = flaggedTransactions;
        this.distinctRules = distinctRules;
        this.totalFlaggedAmount = totalFlaggedAmount;
        this.anyActuallyFraud = anyActuallyFraud;
        this.detailJson = detailJson;
    }

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public String getAccountId() { return accountId; }
    public String getConfidence() { return confidence; }
    public int getFlaggedTransactions() { return flaggedTransactions; }
    public int getDistinctRules() { return distinctRules; }
    public BigDecimal getTotalFlaggedAmount() { return totalFlaggedAmount; }
    public Boolean getAnyActuallyFraud() { return anyActuallyFraud; }
    public String getDetailJson() { return detailJson; }
}