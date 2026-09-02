package com.hassan.anomaly.ingest;

import java.time.Instant;
import java.util.List;

public record JobView(
        Long id,
        String filename,
        JobStatus status,
        int percentComplete,
        Instant createdAt,
        Instant finishedAt,
        long rowsRead,
        long rowsAccepted,
        long rowsRejected,
        int flaggedTransactions,
        int flaggedAccounts,
        boolean hasGroundTruth,
        Double precision,
        Double recall,
        List<ParseError> errors,
        boolean errorsTruncated,
        String failureReason
) {}