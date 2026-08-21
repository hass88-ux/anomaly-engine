# Transaction Anomaly Detection Engine

A rule-based fraud detection engine evaluated against synthetic transaction data
with planted fraud patterns, so precision and recall can be measured directly
rather than estimated.

## Status
In progress. Engine, metrics, data generator, and two rule types implemented and
tuned. Combined rule evaluation, tests, a REST API, and a tuning UI to follow.

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
| Burst | fraud | 6 txns, 40s apart, 2–5x normal amount | Fraud the velocity rule should catch |
| Card testing | fraud | 8 txns, 11h apart, small amounts | Fraud velocity **cannot** catch by design |
| Shopping trip | legitimate | 4 txns, 2min apart, normal amounts | Legitimate behaviour that resembles fraud |

Card testing exists so recall cannot reach 1.00 from a velocity rule alone — a
single rule does not catch all fraud, and a project reporting perfect recall is
measuring its own assumptions. Shopping trips exist so precision is contested:
every false positive is a real customer declined at a till.

Fraud rate is held near 1% (0.79% in the current seed), roughly in line with
published card fraud rates. At higher rates precision becomes trivially easy.

## Results

18,571 transactions, 146 fraudulent (0.79%), seed 42.

### Velocity rule

Counts prior transactions from the same account within a time window.

| Rule config | Precision | Recall | TP | FP | FN |
|---|---|---|---|---|---|
| Velocity(3, 10min) | 0.54 | 0.23 | 33 | 28 | 113 |
| Velocity(4, 10min) | 1.00 | 0.15 | 22 | 0 | 124 |
| Velocity(3, 3min) | 1.00 | 0.23 | 33 | 0 | 113 |
| Velocity(2, 2min) | 1.00 | 0.30 | 44 | 0 | 102 |

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

**Why velocity recall is capped.** Card testing is roughly 55% of planted fraud and
is structurally invisible to a velocity rule — 11 hours between transactions falls
outside any useful window. The rule also requires N priors before firing, so the
opening transactions of every burst pass silently: it catches the tail of an
attack, never the head.

### Amount outlier rule

Compares a transaction against the mean of that account's own prior transactions,
flagging deviation in **either** direction. Silent until the account has a minimum
number of priors, since a baseline computed from two data points is not a baseline.

| Rule config | Precision | Recall | TP | FP | FN |
|---|---|---|---|---|---|
| AmountOutlier(3.0, min 5) | 0.77 | 0.61 | 89 | 26 | 57 |
| AmountOutlier(4.0, min 5) | **0.96** | **0.54** | 79 | 3 | 67 |
| AmountOutlier(6.0, min 5) | 0.99 | 0.49 | 71 | 1 | 75 |

**Reading these.** This rule outperforms velocity on recall by a wide margin
(0.54 vs 0.30 at comparable precision) because it catches card testing, which is
the majority of planted fraud and structurally invisible to a velocity rule. Card
testing amounts sit 10–25x below the account baseline, so they clear even a strict
threshold comfortably.

The exchange rate between thresholds is where the useful finding is:

- 6.0 → 4.0 buys **8 true positives for 2 false positives**
- 4.0 → 3.0 buys **10 true positives for 23 false positives**

That inflection marks the point where fraud amounts stop being separable from
normal spending variance. Below roughly 4x, the density of legitimate transactions
in the band exceeds the density of fraudulent ones, so loosening further costs more
than it returns. **4.0 is the operating point**, and the data says so rather than
the choice being asserted.

Framed operationally: at 4x an analyst receives 82 alerts of which 79 are real; at
3x they receive 115 of which 26 are wasted, in exchange for 10 additional catches.
Whether that trade is worth making depends on the cost of a missed fraud against
the cost of an investigation — a business decision, not an engineering one. The
engine's job is to make the trade-off legible.

Unlike the velocity rule's precision figures, the 3 false positives at 4.0 arise
from genuine variance in normal spending rather than a dataset artifact, which
makes this a more trustworthy number.

## Design notes
- Rules sit behind a `Rule` interface with parameters injected via constructor, so
  one implementation runs under many configurations. Every result above required
  no code changes beyond constructor arguments.
- The amount rule uses a **per-account** baseline rather than a global one. A
  global average would judge a customer who normally spends £15 and one who
  normally spends £200 by the same yardstick, flagging everything from the second
  and nothing wrong with the first.
- Deviation is checked in both directions. Instinct says fraud means large amounts,
  but card testing is anomalous on the *low* side — small probe transactions
  verifying a stolen card is live.
- The rule stays silent below a minimum history threshold. A rule firing
  confidently on two data points is detecting sparse data, not anomalies.
- `BigDecimal` is used for storing and summing money, but converted to `double` for
  computing means. Rounding error accumulates when summing balances; it is
  irrelevant when asking whether a value is roughly 4x an average.
- The replay loop appends each transaction to history *after* evaluating it, so no
  rule can see the future. Reversing those two lines would inflate every metric in
  this README.
- Rules never read the `isFraud` label. Scoring happens outside the engine, in the
  replay loop. Nothing in the language enforces this yet; making it structural is a
  planned change.
- Generated transactions are sorted chronologically before replay — accounts are
  built one at a time, so unsorted data would place future events in a
  transaction's history.

## Known limitations
- Rule evaluation is O(n²): full history is scanned for every transaction.
  Acceptable at ~18k transactions, not beyond. A per-account sliding window with
  eviction is the fix.
- Rules are evaluated in isolation. Combined evaluation is the next step.
- Legitimate burst behaviour is modelled by a single fixed pattern, which
  overstates velocity precision.
- No automated tests yet.

## Stack
Java 21, Maven. Spring Boot, MySQL, and a React tuning UI planned.

## Running
Clone, import as a Maven project, run `com.hassan.anomaly.Main`.