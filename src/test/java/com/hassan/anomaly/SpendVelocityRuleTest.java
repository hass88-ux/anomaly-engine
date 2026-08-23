package com.hassan.anomaly;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SpendVelocityRuleTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private TransactionView txn(String account, String amount, long secondsOffset) {
        return new TransactionView("id" + secondsOffset, account,
                T0.plusSeconds(secondsOffset), new BigDecimal(amount), "CA", 43.65, -79.38);
    }

    private List<TransactionView> baseline(String account, String amount, int count) {
        List<TransactionView> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(txn(account, amount, -86400L * (i + 1)));
        }
        return out;
    }

    @Test
    void flagsHighSpendBurst() {
        Rule rule = new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0);
        List<TransactionView> history = baseline("acc-1", "50.00", 5);
        history = new ArrayList<>(history);
        history.add(txn("acc-1", "150.00", 0));
        history.add(txn("acc-1", "150.00", 40));
        history.add(txn("acc-1", "150.00", 80));

        assertTrue(rule.isSuspicious(txn("acc-1", "150.00", 120), history));
    }

    @Test
    void allowsNormalSpendAtSameTempo() {
        Rule rule = new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0);
        List<TransactionView> history = new ArrayList<>(baseline("acc-1", "50.00", 5));
        history.add(txn("acc-1", "45.00", 0));
        history.add(txn("acc-1", "45.00", 45));
        history.add(txn("acc-1", "45.00", 90));

        assertFalse(rule.isSuspicious(txn("acc-1", "45.00", 135), history));
    }

    @Test
    void staysSilentBelowMinimumCount() {
        Rule rule = new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0);
        List<TransactionView> history = new ArrayList<>(baseline("acc-1", "50.00", 5));
        history.add(txn("acc-1", "500.00", 0));

        assertFalse(rule.isSuspicious(txn("acc-1", "500.00", 40), history));
    }

    @Test
    void ignoresSpendOutsideWindow() {
        Rule rule = new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0);
        List<TransactionView> history = new ArrayList<>(baseline("acc-1", "50.00", 5));
        history.add(txn("acc-1", "300.00", 0));
        history.add(txn("acc-1", "300.00", 40));
        history.add(txn("acc-1", "300.00", 80));

        assertFalse(rule.isSuspicious(txn("acc-1", "300.00", 7200), history));
    }

    @Test
    void ignoresOtherAccounts() {
        Rule rule = new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0);
        List<TransactionView> history = new ArrayList<>(baseline("acc-1", "50.00", 5));
        history.add(txn("acc-2", "500.00", 0));
        history.add(txn("acc-2", "500.00", 40));
        history.add(txn("acc-2", "500.00", 80));

        assertFalse(rule.isSuspicious(txn("acc-1", "500.00", 120), history));
    }

    @Test
    void staysSilentWithNoHistory() {
        Rule rule = new SpendVelocityRule(3, Duration.ofMinutes(3), 6.0);

        assertFalse(rule.isSuspicious(txn("acc-1", "5000.00", 0), List.of()));
    }
}