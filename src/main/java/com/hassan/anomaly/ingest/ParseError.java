package com.hassan.anomaly.ingest;

public record ParseError(long line, String column, String message) {}