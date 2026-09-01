package com.hassan.anomaly;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "replay_run")
public class ReplayRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant runAt;

    @Column(nullable = false)
    private String username;

    private int accounts;
    private int days;
    private long seed;
    private int velocityMinCount;
    private int velocityWindowMinutes;
    private double velocitySpendMultiplier;
    private double amountMultiplier;
    private int amountMinHistory;
    private double geoMaxSpeedKmh;

    private int transactions;
    private int fraudCount;
    private long replayTimeMs;
    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;

    @Column(name = "precision_score")
    private double precision;

    @Column(name = "recall_score")
    private double recall;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String breakdownJson;

    protected ReplayRun() {
    }

    public ReplayRun(Instant runAt, String username, ReplayRequest request,
                     ReplayResult result, String breakdownJson) {
        this.runAt = runAt;
        this.username = username;
        this.accounts = request.accounts();
        this.days = request.days();
        this.seed = request.seed();
        this.velocityMinCount = request.velocityMinCount();
        this.velocityWindowMinutes = request.velocityWindowMinutes();
        this.velocitySpendMultiplier = request.velocitySpendMultiplier();
        this.amountMultiplier = request.amountMultiplier();
        this.amountMinHistory = request.amountMinHistory();
        this.geoMaxSpeedKmh = request.geoMaxSpeedKmh();
        this.transactions = result.transactions();
        this.fraudCount = result.fraudCount();
        this.replayTimeMs = result.replayTimeMs();
        this.truePositives = result.truePositives();
        this.falsePositives = result.falsePositives();
        this.trueNegatives = result.trueNegatives();
        this.falseNegatives = result.falseNegatives();
        this.precision = result.precision();
        this.recall = result.recall();
        this.breakdownJson = breakdownJson;
    }

    public Long getId() { return id; }
    public Instant getRunAt() { return runAt; }
    public String getUsername() { return username; }
    public int getAccounts() { return accounts; }
    public int getDays() { return days; }
    public long getSeed() { return seed; }
    public int getVelocityMinCount() { return velocityMinCount; }
    public int getVelocityWindowMinutes() { return velocityWindowMinutes; }
    public double getVelocitySpendMultiplier() { return velocitySpendMultiplier; }
    public double getAmountMultiplier() { return amountMultiplier; }
    public int getAmountMinHistory() { return amountMinHistory; }
    public double getGeoMaxSpeedKmh() { return geoMaxSpeedKmh; }
    public int getTransactions() { return transactions; }
    public int getFraudCount() { return fraudCount; }
    public long getReplayTimeMs() { return replayTimeMs; }
    public int getTruePositives() { return truePositives; }
    public int getFalsePositives() { return falsePositives; }
    public int getTrueNegatives() { return trueNegatives; }
    public int getFalseNegatives() { return falseNegatives; }
    public double getPrecision() { return precision; }
    public double getRecall() { return recall; }
    public String getBreakdownJson() { return breakdownJson; }
}