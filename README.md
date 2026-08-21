# Transaction Anomaly Detection Engine

A rule-based fraud detection engine evaluated against synthetic transaction data
with planted fraud patterns, so precision and recall can be measured directly
rather than estimated.

## Status
In progress. Engine, metrics, data generator, and velocity rule tuning complete.
Additional rule types, a REST API, and a tuning UI to follow.

## Why synthetic data
Real fraud labels arrive weeks late via chargebacks, so a live system cannot
measure its own accuracy in the moment. Generating the data means ground truth is
known at evaluation time and rule changes can be measured rather than guessed at.
The labels are planted by design — this is a controlled experiment, not a claim
about real-world fraud rates.

The dataset is seeded (`new DataGenerator(42)`), so every run produces identical
data. Without this, a metric moving between runs could mean an improved rule or
just a different random draw.

## Dataset design
Three patterns are planted, chosen so the results are not circular:

| Pattern | Label | Shape | Purpose |
|---|---|---|---|
| Burst | fraud | 6 txns, 40s apart, inflated amounts | Fraud the velocity rule should catch |
| Card testing | fraud | 8 txns, 11h apart, small amounts | Fraud velocity **cannot** catch by design |
| Shopping trip | legitimate | 4 txns, 2min apart, normal amounts | Legitimate behaviour that resembles fraud |

Card testing exists so recall cannot reach 1.00 — a single rule does not catch all
fraud, and a project reporting perfect recall is measuring its own assumptions.
Shopping trips exist so precision is contested: every false positive is a real
customer declined at a till.

Fraud rate is held near 1% (0.79% in the current seed), roughly in line with
published card fraud rates. At higher rates precision becomes trivially easy.

## Results

18,571 transactions, 146 fraudulent (0.79%), seed 42.

| Rule config | Precision | Recall | TP | FP | FN |
|---|---|---|---|---|---|
| Velocity(3, 10min) | 0.54 | 0.23 | 33 | 28 | 113 |
| Velocity(4, 10min) | 1.00 | 0.15 | 22 | 0 | 124 |
| Velocity(3, 3min) | 1.00 | 0.23 | 33 | 0 | 113 |
| Velocity(2, 2min) | 1.00 | **0.30** | 44 | 0 | 102 |

**Reading these.** Config 3 retains every true positive the baseline caught while
eliminating all 28 false positives — a strict improvement rather than a trade-off,
which is uncommon in this kind of tuning.

The mechanism matters more than the headline number. Config 2 also reaches
precision 1.00, but only because shopping trips contain exactly 4 transactions and
a threshold of 4 is structurally unreachable for them — widen the decoy to 5 and it
collapses. Config 3 works because fraud bursts and human shopping have different
*tempo* (~40s apart versus ~2min), which is a property of the behaviour rather than
of the test fixture. It survives changes to decoy length; config 2 does not.

Config 4 scores highest on recall, but its precision rests on an exact-boundary
coincidence: decoy spacing (2min) equals the window (2min), and the strict
`isAfter` comparison excludes transactions sitting precisely on the cutoff. Shift
decoy spacing to 1m50s and false positives reappear. Config 3 excludes decoys by a
full minute of margin, so it is the more defensible configuration despite lower
recall — a rule whose accuracy depends on a tie-break is not one to deploy.

**Why precision 1.00 should not be taken at face value.** The generator produces
only one shape of legitimate burst. Real traffic contains checkout retries, split
payments, and other rapid legitimate sequences this dataset does not model, so
precision would degrade against production data. The figure reflects a limited
decoy population, not a solved problem.

**Why recall is capped.** Card testing is roughly 55% of planted fraud and is
structurally invisible to a velocity rule — 11 hours between transactions falls
outside any useful window. The rule also requires N priors before firing, so the
opening transactions of every burst pass silently: it catches the tail of an
attack, never the head. Raising recall requires a different rule type, not a
better-tuned velocity rule.

## Design notes
- Rules sit behind a `Rule` interface with parameters injected via constructor, so
  one implementation runs under many configurations. The four results above
  required no code changes beyond constructor arguments.
- The replay loop appends each transaction to history *after* evaluating it, so no
  rule can see the future. Reversing those two lines would inflate every metric in
  this README.
- Rules never read the `isFraud` label. Scoring happens outside the engine, in the
  replay loop. Nothing in the language enforces this yet; making it structural is a
  planned change.
- Money is `BigDecimal`, never `double`. Timestamps are `Instant` in UTC.
- Generated transactions are sorted chronologically before replay — accounts are
  built one at a time, so unsorted data would place future events in a
  transaction's history.

## Known limitations
- Rule evaluation is O(n²): full history is scanned for every transaction.
  Acceptable at ~18k transactions, not beyond. A per-account sliding window with
  eviction is the fix.
- One rule type implemented. Recall is capped by this, not by tuning.
- Legitimate burst behaviour is modelled by a single fixed pattern, which
  overstates precision.

## Stack
Java 21, Maven. Spring Boot, MySQL, and a React tuning UI planned.

## Running
Clone, import as a Maven project, run `com.hassan.anomaly.Main`.