package com.hassan.anomaly.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class TransactionParser {

    public static final int MAX_REPORTED_ERRORS = 100;

    private final ColumnMapping mapping;
    private final long maxRows;

    private final List<ParseError> errors = new ArrayList<>();
    private long rowsRead;
    private long rowsAccepted;
    private long rowsRejected;

    public TransactionParser(ColumnMapping mapping, long maxRows) {
        this.mapping = mapping;
        this.maxRows = maxRows;
    }

    public void parse(InputStream in, Consumer<ParsedTransaction> consumer) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(new BomStrippingStream(in), StandardCharsets.UTF_8));
                CSVParser parser = CSVParser.parse(reader, format)) {
            for (CSVRecord record : parser) {
                if (rowsRead >= maxRows) {
                    addError(record.getRecordNumber(), null,
                            "row limit of " + maxRows + " reached - remaining rows ignored");
                    break;
                }

                rowsRead++;

                try {
                    consumer.accept(toTransaction(record));
                    rowsAccepted++;
                } catch (IllegalArgumentException e) {
                    rowsRejected++;
                    addError(record.getRecordNumber(), null, e.getMessage());
                }
            }
        }
    }

    private ParsedTransaction toTransaction(CSVRecord record) {
        String accountId = required(record, mapping.accountId(), "account");
        Instant occurredAt = ValueCoercion.toInstant(get(record, mapping.occurredAt()));
        BigDecimal amount = ValueCoercion.toAmount(get(record, mapping.amount()));

        String transactionId = mapping.transactionId() == null
                ? "row-" + record.getRecordNumber()
                : required(record, mapping.transactionId(), "transaction id");

        Double latitude = null;
        Double longitude = null;
        if (mapping.hasGeo()) {
            String rawLat = get(record, mapping.latitude());
            String rawLon = get(record, mapping.longitude());
            if (!rawLat.isBlank() && !rawLon.isBlank()) {
                latitude = ValueCoercion.toCoordinate(rawLat);
                longitude = ValueCoercion.toCoordinate(rawLon);
            }
        }

        Boolean isFraud = null;
        if (mapping.hasGroundTruth()) {
            String raw = get(record, mapping.isFraud());
            if (!raw.isBlank()) {
                isFraud = ValueCoercion.toBoolean(raw);
            }
        }

        return new ParsedTransaction(
                transactionId, accountId, occurredAt, amount,
                latitude, longitude, isFraud);
    }

    private String get(CSVRecord record, String column) {
        if (column == null || !record.isMapped(column)) {
            return "";
        }
        String value = record.get(column);
        return value == null ? "" : value;
    }

    private String required(CSVRecord record, String column, String label) {
        String value = get(record, column);
        if (value.isBlank()) {
            throw new IllegalArgumentException("missing " + label);
        }
        return value;
    }

    private void addError(long line, String column, String message) {
        if (errors.size() < MAX_REPORTED_ERRORS) {
            errors.add(new ParseError(line, column, message));
        }
    }

    public List<ParseError> errors() {
        return List.copyOf(errors);
    }

    public long rowsRead() {
        return rowsRead;
    }

    public long rowsAccepted() {
        return rowsAccepted;
    }

    public long rowsRejected() {
        return rowsRejected;
    }

    public boolean errorsTruncated() {
        return rowsRejected > errors.size();
    }
}