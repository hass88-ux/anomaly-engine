package com.hassan.anomaly.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValueCoercionTest {

    // ---------- amounts ----------

    @Test
    @DisplayName("strips currency symbols and thousands separators")
    void plainAmounts() {
        assertEquals(new BigDecimal("1234.56"), ValueCoercion.toAmount("$1,234.56"));
        assertEquals(new BigDecimal("1234.56"), ValueCoercion.toAmount("1234.56"));
        assertEquals(new BigDecimal("1234.56"), ValueCoercion.toAmount("CAD 1,234.56"));
        assertEquals(new BigDecimal("99"), ValueCoercion.toAmount("£99"));
    }

    @Test
    @DisplayName("European format: last separator is the decimal point")
    void europeanAmounts() {
        // 1.234,56 and 1,234.56 are the same number under different conventions.
        // Whichever separator appears last is the decimal point.
        assertEquals(new BigDecimal("1234.56"), ValueCoercion.toAmount("1.234,56"));
        assertEquals(new BigDecimal("1234567.89"), ValueCoercion.toAmount("1.234.567,89"));
        assertEquals(new BigDecimal("1234567.89"), ValueCoercion.toAmount("1,234,567.89"));
    }

    @Test
    @DisplayName("accounting parentheses mean negative")
    void negativeAmounts() {
        assertEquals(new BigDecimal("-45.00"), ValueCoercion.toAmount("($45.00)"));
        assertEquals(new BigDecimal("-45.00"), ValueCoercion.toAmount("-45.00"));
    }

    @Test
    @DisplayName("rejects amounts with no digits")
    void unparseableAmounts() {
        assertThrows(IllegalArgumentException.class, () -> ValueCoercion.toAmount("abc"));
        assertThrows(IllegalArgumentException.class, () -> ValueCoercion.toAmount(""));
        assertThrows(IllegalArgumentException.class, () -> ValueCoercion.toAmount("  "));
        assertThrows(IllegalArgumentException.class, () -> ValueCoercion.toAmount(null));
        assertThrows(IllegalArgumentException.class, () -> ValueCoercion.toAmount("$"));
    }

    // ---------- timestamps ----------

    @ParameterizedTest
    @DisplayName("parses the date formats real exports use")
    @ValueSource(strings = {
        "2026-07-15T14:30:00Z",
        "2026-07-15T14:30:00+00:00",
        "2026-07-15T14:30:00",
        "2026-07-15 14:30:00",
        "2026-07-15 14:30",
        "2026/07/15 14:30:00",
        "15/07/2026 14:30:00",
        "15/07/2026 14:30",
        "15-07-2026 14:30:00",
    })
    void dateTimeFormats(String raw) {
        Instant parsed = ValueCoercion.toInstant(raw);
        assertEquals(Instant.parse("2026-07-15T14:30:00Z"), parsed,
                "all of these describe the same moment in UTC");
    }

    @Test
    @DisplayName("date-only values fall back to midnight UTC")
    void dateOnlyFormats() {
        Instant midnight = Instant.parse("2026-07-15T00:00:00Z");
        assertEquals(midnight, ValueCoercion.toInstant("2026-07-15"));
        assertEquals(midnight, ValueCoercion.toInstant("15/07/2026"));
        assertEquals(midnight, ValueCoercion.toInstant("15-07-2026"));
    }

    @Test
    @DisplayName("Unix epoch seconds and milliseconds are distinguished by length")
    void epochFormats() {
        assertEquals(Instant.ofEpochSecond(1_784_125_800L),
                ValueCoercion.toInstant("1784125800"));
        assertEquals(Instant.ofEpochMilli(1_784_125_800_000L),
                ValueCoercion.toInstant("1784125800000"));
    }

    @Test
    @DisplayName("rejects timestamps it cannot recognise")
    void unparseableDates() {
        assertThrows(IllegalArgumentException.class,
                () -> ValueCoercion.toInstant("not-a-date"));
        assertThrows(IllegalArgumentException.class, () -> ValueCoercion.toInstant(""));
        assertThrows(IllegalArgumentException.class, () -> ValueCoercion.toInstant(null));
        assertThrows(IllegalArgumentException.class,
                () -> ValueCoercion.toInstant("2026-13-45"));
    }

    @Test
    @DisplayName("day-first is preferred where the format is ambiguous")
    void ambiguousDatesResolveDayFirst() {
        // 03/04/2026 is 3 April under dd/MM and 4 March under MM/dd.
        // Nothing in the file distinguishes them; day-first is the documented choice.
        assertEquals(Instant.parse("2026-04-03T10:00:00Z"),
                ValueCoercion.toInstant("03/04/2026 10:00"));
    }

    // ---------- booleans ----------

    @ParameterizedTest
    @DisplayName("recognises the many ways a file says yes")
    @ValueSource(strings = {"1", "true", "TRUE", "t", "yes", "Yes", "y",
                            "fraud", "fraudulent", "chargeback"})
    void truthyValues(String raw) {
        assertTrue(ValueCoercion.toBoolean(raw));
    }

    @ParameterizedTest
    @DisplayName("recognises the many ways a file says no")
    @CsvSource({"0", "false", "FALSE", "f", "no", "n", "legit", "legitimate", "clean"})
    void falsyValues(String raw) {
        assertEquals(false, ValueCoercion.toBoolean(raw));
    }

    @Test
    @DisplayName("rejects values it cannot interpret rather than guessing")
    void unparseableBooleans() {
        assertThrows(IllegalArgumentException.class,
                () -> ValueCoercion.toBoolean("maybe"));
        assertThrows(IllegalArgumentException.class,
                () -> ValueCoercion.toBoolean("2"));
    }

    // ---------- coordinates ----------

    @Test
    @DisplayName("parses coordinates including negatives")
    void coordinates() {
        assertEquals(43.65, ValueCoercion.toCoordinate("43.65"), 0.0001);
        assertEquals(-79.38, ValueCoercion.toCoordinate("-79.38"), 0.0001);
        assertEquals(0.0, ValueCoercion.toCoordinate("0"), 0.0001);
    }

    @Test
    @DisplayName("rejects empty and non-numeric coordinates")
    void unparseableCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> ValueCoercion.toCoordinate(""));
        assertThrows(NumberFormatException.class,
                () -> ValueCoercion.toCoordinate("north"));
    }
}