package com.hassan.anomaly;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AmountOutlierRuleTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private Transaction txn(String account, String amount, long secondsOffset) {
        return new Transaction("id" + secondsOffset, account,
                T0.plusSeconds(secondsOffset), new BigDecimal(amount), "CA", false);
    }

    private List<Transaction> baselineOf(String account, String amount, int count) {
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(txn(account, amount, i * 60L));
        }
        return out;
    }

    @Test
    void flagsUnusuallyLargeAmount() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        List<Transaction> history = baselineOf("acc-1", "50.00", 5);

        assertTrue(rule.isSuspicious(txn("acc-1", "500.00", 600), history));
    }

    @Test
    void flagsUnusuallySmallAmount() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        List<Transaction> history = baselineOf("acc-1", "50.00", 5);

        assertTrue(rule.isSuspicious(txn("acc-1", "2.00", 600), history));
    }

    @Test
    void allowsNormalAmount() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        List<Transaction> history = baselineOf("acc-1", "50.00", 5);

        assertFalse(rule.isSuspicious(txn("acc-1", "60.00", 600), history));
    }

    @Test
    void staysSilentBelowMinimumHistory() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        List<Transaction> history = baselineOf("acc-1", "50.00", 3);

        assertFalse(rule.isSuspicious(txn("acc-1", "5000.00", 600), history));
    }

    @Test
    void ignoresOtherAccountsWhenBuildingBaseline() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        List<Transaction> history = baselineOf("acc-2", "50.00", 5);

        assertFalse(rule.isSuspicious(txn("acc-1", "500.00", 600), history));
    }
}