package com.hassan.anomaly;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class DataGenerator {

    private record City(String name, double lat, double lon) {}

    private static final List<City> CITIES = List.of(
            new City("Toronto",   43.65, -79.38),
            new City("Vancouver", 49.28, -123.12),
            new City("Montreal",  45.50, -73.57),
            new City("Calgary",   51.05, -114.07),
            new City("Halifax",   44.65, -63.58));

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
            City home = CITIES.get(random.nextInt(CITIES.size()));

            for (int day = 0; day < daysToSimulate; day++) {
                int perDay = random.nextInt(4);
                for (int i = 0; i < perDay; i++) {
                    Instant when = start
                            .plus(Duration.ofDays(day))
                            .plus(Duration.ofMinutes(random.nextInt(1440)));
                    out.add(txn(id++, account, when,
                            baseAmount * (0.5 + random.nextDouble()),
                            jitter(home), false, "NONE"));
                }
            }

            if (random.nextDouble() < 0.04) {
                out.addAll(burst(account, id, baseAmount, home));
                id += 6;
            }
            if (random.nextDouble() < 0.04) {
                out.addAll(cardTesting(account, id, baseAmount, home));
                id += 8;
            }
            if (random.nextDouble() < 0.02) {
                out.addAll(impossibleTravel(account, id, baseAmount, home));
                id += 2;
            }
            if (random.nextDouble() < 0.15) {
                out.addAll(shoppingTrip(account, id, baseAmount, home));
                id += 4;
            }
        }

        out.sort(Comparator.comparing(Transaction::occurredAt));
        return out;
    }

    private List<Transaction> burst(String account, int id, double base, City home) {
        Instant when = randomMoment();
        double[] location = jitter(home);
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            out.add(txn(id + i, account, when.plus(Duration.ofSeconds(40L * i)),
                    base * (2 + random.nextDouble() * 3), location, true, "BURST"));
        }
        return out;
    }

    private List<Transaction> cardTesting(String account, int id, double base, City home) {
        Instant when = randomMoment();
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            out.add(txn(id + i, account, when.plus(Duration.ofHours(11L * i)),
                    1 + random.nextDouble() * 3, jitter(home), true, "CARD_TESTING"));
        }
        return out;
    }

    private List<Transaction> impossibleTravel(String account, int id, double base, City home) {
        Instant when = randomMoment();
        City far = farFrom(home);
        return List.of(
                txn(id, account, when,
                        base * (0.5 + random.nextDouble()), jitter(home), true, "IMPOSSIBLE_TRAVEL"),
                txn(id + 1, account, when.plus(Duration.ofMinutes(25)),
                        base * (0.5 + random.nextDouble()), jitter(far), true, "IMPOSSIBLE_TRAVEL"));
    }

    private List<Transaction> shoppingTrip(String account, int id, double base, City home) {
        Instant when = randomMoment();
        double[] location = jitter(home);
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            out.add(txn(id + i, account, when.plus(Duration.ofSeconds(45L * i)),
                    base * (0.4 + random.nextDouble()), location, false, "SHOPPING_TRIP"));
        }
        return out;
    }

    private City farFrom(City home) {
        City candidate;
        do {
            candidate = CITIES.get(random.nextInt(CITIES.size()));
        } while (candidate.equals(home));
        return candidate;
    }

    private Instant randomMoment() {
        return start.plus(Duration.ofMinutes(random.nextInt(60 * 24 * 30)));
    }

    private double[] jitter(City city) {
        return new double[] {
                city.lat() + (random.nextDouble() - 0.5) * 0.2,
                city.lon() + (random.nextDouble() - 0.5) * 0.2 };
    }

    private Transaction txn(int id, String account, Instant when,
                            double amount, double[] location,
                            boolean fraud, String pattern) {
        return new Transaction("t" + id, account, when,
                BigDecimal.valueOf(Math.round(amount * 100) / 100.0),
                "CA", location[0], location[1], fraud, pattern);
    }
}