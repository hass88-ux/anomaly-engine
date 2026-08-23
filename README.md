# Transaction Anomaly Detection Engine

A rule-based fraud detection engine evaluated against synthetic transaction data
with planted fraud patterns, so precision and recall can be measured directly
rather than estimated.

Four planted attack shapes, four rule types, per-pattern detection rates, and a
documented case where a rule's apparent accuracy turned out to be an artifact of
the data generator.

**Current configuration: precision 0.93, recall 0.70** across 18,450 transactions
at a 1.04% fraud rate.

## Status
Engine, data generator, four rule types, combined evaluation, per-rule and
per-pattern attribution, and 22 unit tests. A REST API and tuning UI are planned.

## Why synthetic data
Real fraud labels arrive weeks late via chargebacks, so a live system cannot
measure its own accuracy in the moment. Generating the data means ground truth is
known at evaluation time and rule changes can be measured rather than guessed at.
The labels are planted by design — this is a controlled experiment, not a claim
about real-world fraud rates.

The dataset is seeded (`new DataGenerator(42)`), so every run produces identical
data. Without this, a metric moving between runs could mean an improved rule or
just a different random draw.

Pattern frequencies are calibrated to hold the overall fraud rate near 1%, roughly
in line with published card fraud rates. Because changing any frequency shifts the
sequence of random draws, results are only comparable within a single generator
configuration.

## Dataset design

| Pattern | Label | Shape | Purpose |
|---|---|---|---|
| Burst | fraud | 6 txns, 40s apart, 2–5x normal amount, one location | Rapid drain of a stolen card |
| Card testing | fraud | 8 txns, 11h apart, small amounts | Verifying a stolen card is live |
| Impossible travel | fraud | 2 txns, 25min apart, different cities | Card used in two places at once |
| Shopping trip | legitimate | 4 txns, 45s apart, normal amounts, one location | Legitimate behaviour resembling a burst |

Each account is assigned a home city from five Canadian metros; normal transactions
cluster within roughly 10km of it. Without a home location, accounts would be
scattered nationwide and impossible travel would be indistinguishable from ordinary
behaviour.

**Location varies with elapsed time.** Transactions seconds apart within a burst or
shopping trip share one location — a card tapped four times at one merchant does not
move. Card testing, at 11-hour intervals, jitters independently. This distinction is
not cosmetic; getting it wrong invalidated an entire set of results (see
*Corrections*).

## Results

18,450 transactions, 192 fraudulent (1.04%), seed 42.

### Per-pattern detection

The most informative table in the project. Aggregate recall hides which attacks are
caught and which are missed.

| Pattern | Caught | Total | Rate | Caught by |
|---|---|---|---|---|
| Card testing | 77 | 80 | 0.96 | AmountOutlier (77) |
| Impossible travel | 11 | 22 | 0.50 | GeoImpossibility (11) |
| Burst | 47 | 90 | 0.52 | SpendVelocity (40), AmountOutlier (7) |
| Shopping trip *(legit)* | 2 | 204 | 0.01 | AmountOutlier (1), SpendVelocity (1) |
| Normal *(legit)* | 8 | 18,054 | 0.0004 | AmountOutlier (7), GeoImpossibility (1) |

Impossible travel sits at exactly 0.50 by construction: the pattern plants two
transactions, and the first has no meaningful prior to compare against, so only the
second can fire. This is a ceiling, not a tuning failure.

Burst is the weakest at 0.52 — the opening transactions of each burst pass before
enough history accumulates.

The false-positive rate on ordinary transactions is 0.04%. Precision alone hides
how rare that is.

### Rule contributions

| Rule | Fires on fraud | Fires on legitimate | Precision |
|---|---|---|---|
| AmountOutlier(4.0, min 5) | 84 | 8 | 0.91 |
| SpendVelocity(3, 3min, x6.0) | 40 | 1 | 0.98 |
| GeoImpossibility(900 km/h) | 11 | 1 | 0.92 |

**The rules are perfectly disjoint** — no transaction is caught by more than one.
Each targets a distinct signature: aggregate spend in a window, individual amount
deviation, and implied travel speed. A burst of four 3x transactions is 12x in
aggregate but each transaction sits below the 4x individual threshold, so
`SpendVelocity` and `AmountOutlier` do not collide despite both reading amounts.

### Spend velocity: replacing count with spend

The original `VelocityRule` counted transactions in a window and ignored what they
were worth. Once the shopping-trip decoy was tightened to 45-second spacing, that
rule fired on 51 legitimate transactions against 45 fraudulent ones — precision 0.47.

Tempo could not separate the populations: bursts are 40s apart, trips 45s. Count
could, but only because trips contain exactly 4 transactions — tuning to that is
brittle, and fails the moment a real shopper makes five purchases.

Amount was the robust separator. Bursts run 2–5x the account baseline, trips
0.4–1.4x, and those distributions barely touch. `SpendVelocityRule` requires both a
minimum count in the window **and** aggregate window spend abnormal against the
account's own mean.

| Multiplier | Fraud caught | Shopping trips hit | Marginal cost |
|---|---|---|---|
| 8.0 | 35 | 0 | — |
| **6.0** | **40** | **1** | 5 TP per 1 FP |
| 4.0 | 45 | 12 | 5 TP per 11 FP |

The 8→6 step buys 5 true positives for 1 false positive. The 6→4 step buys the same
5 for 11. That inflection brackets the point where the spend distributions begin to
overlap. **6.0 is the operating point.**

At 4.0 the rule catches exactly 45 bursts — identical to the original count-based
rule — with 12 false positives against 51. Same detection, a quarter of the cost.

The fix was not a better threshold. It was a rule that stopped discarding
information it already held.

### Amount outlier tuning

*Measured against an earlier generator configuration. Retained because the argument
holds; the absolute figures do not correspond to the current dataset.*

| Rule config | Precision | Recall |
|---|---|---|
| AmountOutlier(3.0, min 5) | 0.77 | 0.61 |
| AmountOutlier(4.0, min 5) | 0.96 | 0.54 |
| AmountOutlier(6.0, min 5) | 0.99 | 0.49 |

- 6.0 → 4.0 bought 8 true positives for 2 false positives
- 4.0 → 3.0 bought 10 true positives for 23 false positives

The same shape as the spend velocity curve: a threshold below which fraud amounts
stop being separable from normal spending variance. Framed operationally, at 4x an
analyst receives 82 alerts of which 79 are real; at 3x, 115 of which 26 are wasted.
Whether that trade is worth making depends on the cost of a missed fraud against the
cost of an investigation — a business decision, not an engineering one. The engine's
job is to make the trade-off legible.

### Original velocity rule tuning

*Superseded by `SpendVelocityRule`. Measured against an earlier generator
configuration and a looser decoy.*

| Rule config | Precision | Recall |
|---|---|---|
| Velocity(3, 10min) | 0.54 | 0.23 |
| Velocity(4, 10min) | 1.00 | 0.15 |
| Velocity(3, 3min) | 1.00 | 0.23 |
| Velocity(2, 2min) | 1.00 | 0.30 |

Config 2 reaches precision 1.00 only because shopping trips contained exactly 4
transactions, making a threshold of 4 structurally unreachable for them — an
artifact of the fixture, not a property of the rule. Config 3 excluded decoys by
tempo instead, which was more defensible until the decoy was tightened and tempo
stopped separating anything.

## Corrections

Three claims in earlier versions of this document were wrong. They are recorded
rather than removed, because the corrections are the more useful content.

**Geo-impossibility precision was an artifact.** The rule initially scored 37 true
positives with 2 false positives and appeared to catch burst transactions as a
bonus. It did not. Positional jitter was being applied per transaction, so six burst
transactions 40 seconds apart sat up to 20km from each other — implying impossible
speeds. Holding location fixed within a burst dropped the rule to 11 true positives
and 1 false positive: exactly the impossible-travel pattern it was designed for, and
nothing else. The apparent bonus detection was the generator, not the rule.

**A hypothesis about rule overlap was half right.** With three rules, 18
transactions fired more than one. The proposed explanation was that geo overlapped
with velocity on bursts. Attribution showed 10 velocity+geo and 8 amount+geo —
and the reasoning that had specifically ruled out amount+geo (card testing's
11-hour gaps make travel trivially possible) was wrong about which transactions
were involved. Both groups were bursts, split by whether velocity had accumulated
enough priors. Once the location artifact was fixed, all 18 overlaps disappeared.

**The shopping-trip decoy tested nothing for several runs.** It was spaced 2 minutes
apart against a 3-minute window, so at most one prior ever fell inside — no rule
could fire on it. Reported precision of 0.99 was measured against a decoy population
that was structurally invisible. Tightening to 45 seconds dropped precision to 0.65
and exposed the count-based velocity rule's real false-positive rate.

The common thread: **every suspiciously good result in this project turned out to be
a property of the test data rather than the detection logic.** Precision above 0.95
was, each time, a signal to inspect the generator.

## Design notes
- Rules sit behind a `Rule` interface with parameters injected via constructor.
  Adding the third and fourth rules required no change to the replay loop, the
  confusion matrix, the attribution report, or any other rule — one more entry in a
  list.
- **Rules cannot read the fraud label — enforced by the type system, not by
  convention.** `Transaction` carries `isFraud` and `fraudPattern`; rules receive a
  `TransactionView`, which has neither. The replay loop strips the labels before
  rules see the data and reads them only when scoring. A rule that peeks at the
  label scores perfectly and proves nothing, and the failure is invisible because
  the output looks excellent. Making it a compile error removes the possibility.
- The replay loop appends each transaction to history *after* evaluating it, so no
  rule can see the future. Reversing those two lines would inflate every metric here.
- The loop evaluates every rule on every transaction rather than short-circuiting.
  Wasted work for a boolean verdict, but a precondition for the attribution that
  produced most of the findings above.
- `SpendVelocityRule` excludes the transaction under judgment from the account
  baseline while including it in the window total. Folding it into its own baseline
  would dilute the signal being tested.
- `AmountOutlierRule` uses a per-account baseline. A global average would judge a
  customer who normally spends £15 and one who normally spends £200 by the same
  yardstick.
- Amount deviation is checked in both directions. Instinct says fraud means large
  amounts, but card testing is anomalous on the *low* side.
- `GeoImpossibilityRule` compares against the immediately previous transaction, not
  a window. Implied speed is meaningful only between consecutive events. It uses
  haversine rather than flat-plane distance — at Canadian latitudes a degree of
  longitude is roughly 75km against 111km for latitude.
- The geo rule guards against zero elapsed time. Identical timestamps would divide
  by zero, produce `Infinity`, and flag every simultaneous pair.
- `BigDecimal` for storing and summing money, converted to `double` for computing
  means. Rounding error accumulates when summing balances; it is irrelevant when
  asking whether a value is roughly 4x an average.
- Timestamps are `Instant` in UTC. Generated transactions are sorted chronologically
  before replay.

## Tests

22 unit tests across all four rules, running in well under a second. The rules have
no framework dependencies — plain Java over lists — so tests need no application
context.

What they pin:

- **Threshold boundaries from both sides**, so a test suite cannot pass against a
  rule that flags everything.
- **The account filter on every rule.** Removing it turns velocity into a global
  counter and gives the amount rule a global baseline — both would still run and
  produce plausible, wrong numbers.
- **Bidirectional amount deviation.** An implementation checking only the upper
  bound fails, and it is the lower bound that catches card testing.
- **Minimum-history and minimum-count guards**, pinning deliberate design decisions
  rather than observed behaviour.
- **Geo compares against the most recent prior, not the oldest.** An account that
  travels Vancouver→Toronto overnight then moves across Toronto is legitimate
  against the recent prior and impossible against the oldest.
- **The zero-elapsed-time guard.**
- **Spend velocity's core justification:** two tests with identical tempo — four
  transactions 40s apart — where one is 3x baseline and one is 0.9x. One fires, one
  does not. The original count-based rule fires on both.

Each test isolates one failure mode, so a red test names what broke. The suite was
verified by deliberately removing the account filter and confirming exactly one test
failed.

The `TransactionView` refactor was validated against it: the change touched the
interface, every rule, the replay loop, and every test class, and combined metrics
came out identical afterwards.

## Known limitations
- Rule evaluation is O(n²): full history is scanned for every transaction, for every
  rule. Acceptable at ~18k transactions, not beyond. A per-account sliding window
  with eviction is the fix.
- Burst recall is 0.52. The opening transactions of each burst pass before enough
  history accumulates, and no current rule addresses the head of an attack.
- The replay loop, confusion matrix, attribution report, and data generator are
  untested. Coverage is limited to rule logic.
- Legitimate behaviour is modelled by two patterns (normal spending and shopping
  trips). Real traffic contains checkout retries, split payments, and subscription
  batches that this dataset does not model, so precision would degrade against
  production data.
- Merchant-category anomalies are unimplemented.
- Alert severity is binary. Real systems score confidence and route accordingly.

## Stack
Java 21, Maven, JUnit 5. Spring Boot, MySQL, and a React tuning UI planned.

## Running
Clone, import as a Maven project, run `com.hassan.anomaly.Main`. Tests run via
`mvn test` or through the IDE.