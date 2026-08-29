package com.hassan.anomaly;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record ReplayRequest(
        @Min(1) @Max(10_000) int accounts,
        @Min(1) @Max(365) int days,
        long seed,
        @Min(2) @Max(100) int velocityMinCount,
        @Min(1) @Max(1440) int velocityWindowMinutes,
        @Positive double velocitySpendMultiplier,
        @Positive double amountMultiplier,
        @Min(1) @Max(1000) int amountMinHistory,
        @Positive double geoMaxSpeedKmh
) {
    public static ReplayRequest defaults() {
        return new ReplayRequest(400, 30, 42L, 3, 3, 6.0, 4.0, 5, 900.0);
    }
}