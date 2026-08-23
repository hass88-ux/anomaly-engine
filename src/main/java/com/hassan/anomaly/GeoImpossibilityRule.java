package com.hassan.anomaly;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GeoImpossibilityRule implements Rule {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final double maxSpeedKmh;

    public GeoImpossibilityRule(double maxSpeedKmh) {
        this.maxSpeedKmh = maxSpeedKmh;
    }

    @Override
    public String name() {
        return "GeoImpossibility(" + maxSpeedKmh + " km/h)";
    }

    @Override
    public boolean isSuspicious(TransactionView txn, List<TransactionView> history) {
        Optional<TransactionView> previous = history.stream()
                .filter(t -> t.accountId().equals(txn.accountId()))
                .max(Comparator.comparing(TransactionView::occurredAt));

        if (previous.isEmpty()) {
            return false;
        }

        TransactionView prior = previous.get();
        Duration elapsed = Duration.between(prior.occurredAt(), txn.occurredAt());
        double hours = elapsed.toMillis() / 3_600_000.0;

        if (hours <= 0) {
            return false;
        }

        double km = distanceKm(prior.latitude(), prior.longitude(),
                               txn.latitude(), txn.longitude());

        return km / hours > maxSpeedKmh;
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}