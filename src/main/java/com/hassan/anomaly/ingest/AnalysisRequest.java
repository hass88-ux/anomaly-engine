package com.hassan.anomaly.ingest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AnalysisRequest(
        @NotNull(message = "mapping is required") ColumnMapping mapping,

        @Min(2) @Max(20) int velocityMinCount,
        @Min(1) @Max(120) int velocityWindowMinutes,
        @Min(1) @Max(20) double velocitySpendMultiplier,

        @Min(1) @Max(20) double amountMultiplier,
        @Min(1) @Max(50) int amountMinHistory,

        @Min(100) @Max(5000) double geoMaxSpeedKmh
) {
    public static AnalysisRequest defaults(ColumnMapping mapping) {
        return new AnalysisRequest(mapping, 3, 3, 4.0, 4.0, 5, 900.0);
    }
}