package com.hassan.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class GeoImpossibilityRuleTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static final double TORONTO_LAT = 43.65, TORONTO_LON = -79.38;
    private static final double VANCOUVER_LAT = 49.28, VANCOUVER_LON = -123.12;

    private TransactionView txn(String account, long secondsOffset,
                                double lat, double lon) {
        return new TransactionView("id" + secondsOffset, account,
                T0.plusSeconds(secondsOffset), new BigDecimal("50.00"), "CA", lat, lon);
    }

    @Test
    void flagsImpossibleTravel() {
        Rule rule = new GeoImpossibilityRule(900);
        List<TransactionView> history = List.of(
                txn("acc-1", 0, TORONTO_LAT, TORONTO_LON));

        assertTrue(rule.isSuspicious(
                txn("acc-1", 1500, VANCOUVER_LAT, VANCOUVER_LON), history));
    }

    @Test
    void allowsSameJourneyGivenEnoughTime() {
        Rule rule = new GeoImpossibilityRule(900);
        List<TransactionView> history = List.of(
                txn("acc-1", 0, TORONTO_LAT, TORONTO_LON));

        assertFalse(rule.isSuspicious(
                txn("acc-1", 6 * 3600, VANCOUVER_LAT, VANCOUVER_LON), history));
    }

    @Test
    void allowsMovementWithinCity() {
        Rule rule = new GeoImpossibilityRule(900);
        List<TransactionView> history = List.of(
                txn("acc-1", 0, TORONTO_LAT, TORONTO_LON));

        assertFalse(rule.isSuspicious(
                txn("acc-1", 3600, 43.70, -79.42), history));
    }

    @Test
    void staysSilentWithNoPriorTransaction() {
        Rule rule = new GeoImpossibilityRule(900);

        assertFalse(rule.isSuspicious(
                txn("acc-1", 0, TORONTO_LAT, TORONTO_LON), List.of()));
    }

    @Test
    void ignoresOtherAccounts() {
        Rule rule = new GeoImpossibilityRule(900);
        List<TransactionView> history = List.of(
                txn("acc-2", 0, TORONTO_LAT, TORONTO_LON));

        assertFalse(rule.isSuspicious(
                txn("acc-1", 1500, VANCOUVER_LAT, VANCOUVER_LON), history));
    }

    @Test
    void comparesAgainstMostRecentPriorNotOldest() {
        Rule rule = new GeoImpossibilityRule(900);
        List<TransactionView> history = List.of(
                txn("acc-1", 0, VANCOUVER_LAT, VANCOUVER_LON),
                txn("acc-1", 20 * 3600, TORONTO_LAT, TORONTO_LON));

        assertFalse(rule.isSuspicious(
                txn("acc-1", 21 * 3600, 43.70, -79.42), history));
    }

    @Test
    void staysSilentWhenTimestampsIdentical() {
        Rule rule = new GeoImpossibilityRule(900);
        List<TransactionView> history = List.of(
                txn("acc-1", 0, TORONTO_LAT, TORONTO_LON));

        assertFalse(rule.isSuspicious(
                txn("acc-1", 0, VANCOUVER_LAT, VANCOUVER_LON), history));
    }
}