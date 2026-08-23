package com.hassan.anomaly;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class VelocityRuleTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private TransactionView txn(String id, String account, long secondsOffset) {
        return new TransactionView(id, account, T0.plusSeconds(secondsOffset),
                new BigDecimal("50.00"), "CA", 43.65, -79.38);
    }

    @Test
    void firesWhenThresholdMet() {
        Rule rule = new VelocityRule(3, Duration.ofMinutes(10));
        List<TransactionView> history = List.of(
                txn("a", "acc-1", 0),
                txn("b", "acc-1", 60),
                txn("c", "acc-1", 120));

        assertTrue(rule.isSuspicious(txn("d", "acc-1", 180), history));
    }

    @Test
    void staysSilentBelowThreshold() {
        Rule rule = new VelocityRule(3, Duration.ofMinutes(10));
        List<TransactionView> history = List.of(
                txn("a", "acc-1", 0),
                txn("b", "acc-1", 60));

        assertFalse(rule.isSuspicious(txn("c", "acc-1", 120), history));
    }

    @Test
    void ignoresOtherAccounts() {
        Rule rule = new VelocityRule(3, Duration.ofMinutes(10));
        List<TransactionView> history = List.of(
                txn("a", "acc-2", 0),
                txn("b", "acc-2", 60),
                txn("c", "acc-2", 120));

        assertFalse(rule.isSuspicious(txn("d", "acc-1", 180), history));
    }

    @Test
    void ignoresTransactionsOutsideWindow() {
        Rule rule = new VelocityRule(3, Duration.ofMinutes(10));
        List<TransactionView> history = List.of(
                txn("a", "acc-1", 0),
                txn("b", "acc-1", 60),
                txn("c", "acc-1", 120));

        assertFalse(rule.isSuspicious(txn("d", "acc-1", 7200), history));
    }
}