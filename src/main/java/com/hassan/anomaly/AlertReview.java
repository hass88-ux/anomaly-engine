package com.hassan.anomaly;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "alert_review",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_alert_review_user_account",
        columnNames = { "username", "account_id" }))
public class AlertReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AlertReview() {
    }

    public AlertReview(String username, String accountId, ReviewStatus status, String note) {
        this.username = username;
        this.accountId = accountId;
        this.status = status;
        this.note = note;
        this.updatedAt = Instant.now();
    }

    public void update(ReviewStatus status, String note) {
        this.status = status;
        this.note = note;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getAccountId() { return accountId; }
    public ReviewStatus getStatus() { return status; }
    public String getNote() { return note; }
    public Instant getUpdatedAt() { return updatedAt; }
}