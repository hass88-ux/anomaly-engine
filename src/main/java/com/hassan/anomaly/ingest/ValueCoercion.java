package com.hassan.anomaly.ingest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ValueCoercion {

    private ValueCoercion() {}

    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ROOT));

    private static final List<DateTimeFormatter> DATE_ONLY_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT));

    private static final Set<String> TRUE_VALUES =
            Set.of("1", "true", "t", "yes", "y", "fraud", "fraudulent", "chargeback");

    private static final Set<String> FALSE_VALUES =
            Set.of("0", "false", "f", "no", "n", "legit", "legitimate", "clean", "");

    public static Instant toInstant(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty timestamp");
        }

        if (value.matches("\\d{10}")) {
            return Instant.ofEpochSecond(Long.parseLong(value));
        }
        if (value.matches("\\d{13}")) {
            return Instant.ofEpochMilli(Long.parseLong(value));
        }

        for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
                if (format == DateTimeFormatter.ISO_INSTANT) {
                    return Instant.parse(value);
                }
                if (format == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    return java.time.OffsetDateTime.parse(value).toInstant();
                }
                return LocalDateTime.parse(value, format).toInstant(ZoneOffset.UTC);
            } catch (Exception ignored) {
                // try the next format
            }
        }

        for (DateTimeFormatter format : DATE_ONLY_FORMATS) {
            try {
                return LocalDate.parse(value, format).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {
                // try the next format
            }
        }

        throw new IllegalArgumentException("unrecognised date format: " + value);
    }

    public static BigDecimal toAmount(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty amount");
        }

        boolean negative = value.startsWith("(") && value.endsWith(")");
        if (negative) {
            value = value.substring(1, value.length() - 1);
        }

        value = value.replaceAll("[^0-9.,\\-]", "");

        int lastComma = value.lastIndexOf(',');
        int lastDot = value.lastIndexOf('.');

        if (lastComma > lastDot) {
            value = value.replace(".", "").replace(',', '.');
        } else {
            value = value.replace(",", "");
        }

        if (value.isEmpty() || value.equals("-")) {
            throw new IllegalArgumentException("no digits in amount");
        }

        BigDecimal amount = new BigDecimal(value);
        return negative ? amount.negate() : amount;
    }

    public static double toCoordinate(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty coordinate");
        }
        return Double.parseDouble(value);
    }

    public static boolean toBoolean(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(value)) {
            return true;
        }
        if (FALSE_VALUES.contains(value)) {
            return false;
        }
        throw new IllegalArgumentException("unrecognised boolean: " + raw);
    }
}