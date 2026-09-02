package com.hassan.anomaly.ingest;

public record ColumnMapping(
        String transactionId,
        String accountId,
        String occurredAt,
        String amount,
        String latitude,
        String longitude,
        String isFraud
) {
    public boolean hasGeo() {
        return latitude != null && !latitude.isBlank()
                && longitude != null && !longitude.isBlank();
    }

    public boolean hasGroundTruth() {
        return isFraud != null && !isFraud.isBlank();
    }

    public boolean hasRequired() {
        return notBlank(accountId) && notBlank(occurredAt) && notBlank(amount);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}