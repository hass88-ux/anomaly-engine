# Transaction Anomaly Detection Engine

A rule-based fraud detection engine evaluated against synthetic transaction data
with planted fraud patterns, allowing precision and recall to be measured directly.

## Status
In progress. Core engine and metrics working; data generator next.

## Why synthetic data
Real fraud labels arrive weeks late via chargebacks. Generating the data means
the ground truth is known at evaluation time, so rule changes can be measured
rather than guessed at.

## Design notes
- Rules sit behind a `Rule` interface with parameters injected via constructor,
  so the same code runs under different configurations.
- The replay loop adds each transaction to history *after* evaluating it, so no
  rule can see the future. Without this, measured accuracy would be meaningless.
- Rules never read the `isFraud` label. Scoring happens outside the engine.
- Money is `BigDecimal`, not `double`. Timestamps are `Instant` in UTC.

## Stack
Java 21, Maven. Spring Boot, MySQL, and a React tuning UI planned.

## Running
Clone, import as a Maven project, run `com.hassan.anomaly.Main`.