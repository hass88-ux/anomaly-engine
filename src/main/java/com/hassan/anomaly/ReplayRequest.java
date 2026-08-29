package com.hassan.anomaly;

public record ReplayRequest(
        int accounts,
        int days,
        long seed,
        int velocityMinCount,
        int velocityWindowMinutes,
        double velocitySpendMultiplier,
        double amountMultiplier,
        int amountMinHistory,
        double geoMaxSpeedKmh
) {
    public static ReplayRequest defaults() {
        return new ReplayRequest(400, 30, 42L, 3, 3, 6.0, 4.0, 5, 900.0);
    }
}