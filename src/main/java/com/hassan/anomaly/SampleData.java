package com.hassan.anomaly;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class SampleData {

    public static List<Transaction> transactions() {
        Instant start = Instant.parse("2026-08-20T10:00:00Z");

        return List.of(
            new Transaction("t1", "acc-1", start,                  new BigDecimal("45.00"), "CA", 43.65, -79.38, false),
            new Transaction("t2", "acc-2", start.plusSeconds(30),   new BigDecimal("12.50"), "CA", 43.70, -79.40, false),
            new Transaction("t3", "acc-1", start.plusSeconds(60),   new BigDecimal("30.00"), "CA", 43.66, -79.39, false),
            new Transaction("t4", "acc-1", start.plusSeconds(120),  new BigDecimal("22.00"), "CA", 43.64, -79.37, false),
            new Transaction("t5", "acc-1", start.plusSeconds(180),  new BigDecimal("18.00"), "CA", 43.65, -79.38, true),
            new Transaction("t6", "acc-1", start.plusSeconds(240),  new BigDecimal("95.00"), "CA", 43.67, -79.41, true),
            new Transaction("t7", "acc-2", start.plusSeconds(3600), new BigDecimal("60.00"), "CA", 43.71, -79.42, false),
            new Transaction("t8", "acc-1", start.plusSeconds(7200), new BigDecimal("40.00"), "CA", 43.65, -79.38, false)
        );
    }
}