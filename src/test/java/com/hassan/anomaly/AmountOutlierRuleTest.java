package com.hassan.anomaly;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class AmountOutlierRuleTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private TransactionView txn(String account, String amount, long secondsOffset) {
        return new TransactionView("id" + secondsOffset, account,
                T0.plusSeconds(secondsOffset), new BigDecimal(amount), "CA", 43.65, -79.38);
    }

    private AccountHistory baselineOf(String account, String amount, int count) {
        AccountHistory history = new AccountHistory();
        for (int i = 0; i < count; i++) {
            history.add(txn(account, amount, i * 60L));
        }
        return history;
    }

    @Test
    void flagsUnusuallyLargeAmount() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        AccountHistory history = baselineOf("acc-1", "50.00", 5);

        assertTrue(rule.isSuspicious(txn("acc-1", "500.00", 600), history));
    }

    @Test
    void flagsUnusuallySmallAmount() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        AccountHistory history = baselineOf("acc-1", "50.00", 5);

        assertTrue(rule.isSuspicious(txn("acc-1", "2.00", 600), history));
    }

    @Test
    void allowsNormalAmount() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        AccountHistory history = baselineOf("acc-1", "50.00", 5);

        assertFalse(rule.isSuspicious(txn("acc-1", "60.00", 600), history));
    }

    @Test
    void staysSilentBelowMinimumHistory() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        AccountHistory history = baselineOf("acc-1", "50.00", 3);

        assertFalse(rule.isSuspicious(txn("acc-1", "5000.00", 600), history));
    }

    @Test
    void ignoresOtherAccountsWhenBuildingBaseline() {
        Rule rule = new AmountOutlierRule(4.0, 5);
        AccountHistory history = baselineOf("acc-2", "50.00", 5);

        assertFalse(rule.isSuspicious(txn("acc-1", "500.00", 600), history));
    }
}