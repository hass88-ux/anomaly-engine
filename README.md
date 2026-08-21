# Transaction Anomaly Detection Engine

A rule-based fraud detection engine evaluated against synthetic transaction data
with planted fraud patterns, so precision and recall can be measured directly
rather than estimated.

## Status
In progress. Engine, metrics, and data generator working. Currently tuning the
velocity rule. Additional rule types, a REST API, and a tuning UI to follow.

## Why synthetic data
Real fraud labels arrive weeks late via chargebacks, so a live system cannot
measure its own accuracy in the moment. Generating the data means ground truth
is known at evaluation time and rule changes can be measured rather than guessed
at. The labels are planted by design — this is a controlled experiment, not a
claim about real-world fraud rates.

The dataset is seeded (`new DataGenerator(42)`), so every run produces identical
data. Without this, a metric moving between runs could mean an improved rule or
just a different random draw.

## Dataset design
Three patterns are planted, chosen so the results are not circular:

| Pattern | Label | Shape | Purpose |
|---|---|---|---|
| Burst | fraud | 6 txns, 40s apart, inflated amounts | Fraud the velocity rule should catch |
| Card testing | fraud | 8 txns, 11h apart, £1–4 each | Fraud velocity **cannot** catch by design |
| Shopping trip | legitimate | 4 txns, 2min apart, normal amounts | Legitimate behaviour that looks like fraud |

Card testing exists so recall cannot reach 1.00. A single rule does not catch all
fraud, and a project reporting perfect recall is measuring its own assumptions.
Shopping trips exist so precision is contested — every false positive is a real
customer declined at a till.

Fraud rate is held near 1%, roughly in line with published card fraud rates. At
higher rates precision becomes trivially easy.

## Results

| Rule config | Precision | Recall | Notes |
|---|---|---|---|
| Velocity(3, 10min) | 1.00 | 0.23 | Pre-fix: decoys spaced 5min fell outside the window, so no false positives were possible. Not a meaningful result. |
| Velocity(3, 10min) | | | Post-fix baseline |
| Velocity(4, 10min) | | | |
| Velocity(3, 3min) | | | |

Recall is structurally capped: card testing is ~55% of planted fraud and is
invisible to a velocity rule. The rule also needs 3 priors before firing, so the
first three transactions of every burst pass silently — it catches the tail of an
attack, never the head.

## Design notes
- Rules sit behind a `Rule` interface with parameters injected via constructor,
  so one implementation runs under many configurations.
- The replay loop appends each transaction to history *after* evaluating it, so
  no rule can see the future. Reversing those two lines would inflate every
  metric in this README.
- Rules never read the `isFraud` label. Scoring happens outside the engine, in
  the replay loop. Nothing in the language enforces this yet; making it
  structural is a planned change.
- Money is `BigDecimal`, never `double`. Timestamps are `Instant` in UTC.
- Generated transactions are sorted chronologically before replay — accounts are
  built one at a time, so unsorted data would place future events in a
  transaction's history.

## Known limitations
- Rule evaluation is O(n²): history is scanned in full for every transaction.
  Acceptable at ~18k transactions, not beyond. A per-account window with eviction
  is the fix.
- One rule type implemented so far.

## Stack
Java 21, Maven. Spring Boot, MySQL, and a React tuning UI planned.

## Running
Clone, import as a Maven project, run `com.hassan.anomaly.Main`.