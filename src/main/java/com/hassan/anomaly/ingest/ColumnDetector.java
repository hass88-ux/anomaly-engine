package com.hassan.anomaly.ingest;

import java.util.List;
import java.util.Locale;

public final class ColumnDetector {

    private ColumnDetector() {}

    private static final List<String> TRANSACTION_ID =
            List.of("transactionid", "transaction_id", "txnid", "txn_id", "id", "reference");

    private static final List<String> ACCOUNT_ID =
            List.of("accountid", "account_id", "account", "customerid", "customer_id",
                    "cardid", "card_id", "userid", "user_id");

    private static final List<String> OCCURRED_AT =
            List.of("occurredat", "occurred_at", "timestamp", "datetime", "date",
                    "time", "createdat", "created_at", "transactiondate", "transaction_date");

    private static final List<String> AMOUNT =
            List.of("amount", "value", "total", "transactionamount", "transaction_amount",
                    "sum", "price");

    private static final List<String> LATITUDE =
            List.of("latitude", "lat");

    private static final List<String> LONGITUDE =
            List.of("longitude", "lon", "lng", "long");

    private static final List<String> IS_FRAUD =
            List.of("isfraud", "is_fraud", "fraud", "fraudulent", "label",
                    "class", "target", "ischargeback", "is_chargeback");

    public static ColumnMapping detect(List<String> headers) {
        return new ColumnMapping(
                match(headers, TRANSACTION_ID),
                match(headers, ACCOUNT_ID),
                match(headers, OCCURRED_AT),
                match(headers, AMOUNT),
                match(headers, LATITUDE),
                match(headers, LONGITUDE),
                match(headers, IS_FRAUD));
    }

    private static String match(List<String> headers, List<String> candidates) {
        for (String candidate : candidates) {
            for (String header : headers) {
                if (normalise(header).equals(candidate)) {
                    return header;
                }
            }
        }

        for (String candidate : candidates) {
            for (String header : headers) {
                if (normalise(header).contains(candidate)) {
                    return header;
                }
            }
        }

        return null;
    }

    private static String normalise(String header) {
        return header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}