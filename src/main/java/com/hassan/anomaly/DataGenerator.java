package com.hassan.anomaly;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class DataGenerator {

    private final Random random;
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");

    public DataGenerator(long seed) {
        this.random = new Random(seed);
    }

    public List<Transaction> generate(int accountCount, int daysToSimulate) {
        List<Transaction> out = new ArrayList<>();
        int id = 0;

        for (int a = 0; a < accountCount; a++) {
            String account = "acc-" + a;
            double baseAmount = 20 + random.nextDouble() * 80;

            for (int day = 0; day < daysToSimulate; day++) {
                int perDay = random.nextInt(4);
                for (int i = 0; i < perDay; i++) {
                    Instant when = start
                            .plus(Duration.ofDays(day))
                            .plus(Duration.ofMinutes(random.nextInt(1440)));
                    out.add(new Transaction("t" + id++, account, when,
                            amount(baseAmount * (0.5 + random.nextDouble())),
                            "CA", false));
                }
            }

            if (random.nextDouble() < 0.04) {
                out.addAll(burst(account, id, baseAmount));
                id += 6;
            }
            if (random.nextDouble() < 0.04) {
                out.addAll(cardTesting(account, id, baseAmount));
                id += 8;
            }
            if (random.nextDouble() < 0.06) {
                out.addAll(shoppingTrip(account, id, baseAmount));
                id += 4;
            }
        }

        out.sort(Comparator.comparing(Transaction::occurredAt));
        return out;
    }

    private List<Transaction> burst(String account, int id, double base) {
        Instant when = randomMoment();
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            out.add(new Transaction("t" + (id + i), account,
                    when.plus(Duration.ofSeconds(40L * i)),
                    amount(base * (2 + random.nextDouble() * 3)),
                    "CA", true));
        }
        return out;
    }

    private List<Transaction> cardTesting(String account, int id, double base) {
        Instant when = randomMoment();
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            out.add(new Transaction("t" + (id + i), account,
                    when.plus(Duration.ofHours(11L * i)),
                    amount(1 + random.nextDouble() * 3),
                    "CA", true));
        }
        return out;
    }

    private List<Transaction> shoppingTrip(String account, int id, double base) {
        Instant when = randomMoment();
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            out.add(new Transaction("t" + (id + i), account,
            		when.plus(Duration.ofMinutes(2L * i)),
                    amount(base * (0.4 + random.nextDouble())),
                    "CA", false));
        }
        return out;
    }

    private Instant randomMoment() {
        return start.plus(Duration.ofMinutes(random.nextInt(60 * 24 * 30)));
    }

    private BigDecimal amount(double value) {
        return BigDecimal.valueOf(Math.round(value * 100) / 100.0);
    }
}