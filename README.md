# Transaction Anomaly Detection Engine

A rule-based fraud detection engine evaluated against synthetic transaction data
with planted fraud patterns, so precision and recall can be measured directly
rather than estimated.

Four planted attack shapes, four rule types, per-pattern detection rates, a 225x
performance rewrite, and a documented case where a rule's apparent accuracy turned
out to be an artifact of the data generator.

**Engine: precision 0.93, recall 0.70** across 18,450 transactions at a 1.04% fraud
rate. Replays 148,000 transactions in 600ms.

## Status
Engine complete (tagged `v1-engine`). A REST API wrapping it is in progress on the
`spring-api` branch: replay endpoints working, input validation and persistence to
follow.

## Why synthetic data
Real fraud labels arrive weeks late via chargebacks, so a live system cannot measure
its own accuracy in the moment. Generating the data means ground truth is known at
evaluation time and rule changes can be measured rather than guessed at. The labels
are planted by design — this is a controlled experiment, not a claim about real-world
fraud rates.

The dataset is seeded, so every run produces identical data. Without this, a metric
moving between runs could mean an improved rule or just a different random draw.

Pattern frequencies are calibrated to hold the overall fraud rate near 1%, roughly in
line with published card fraud rates. Because changing any frequency shifts the
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

The most informative table here. Aggregate recall hides which attacks are caught and
which are missed.

| Pattern | Caught | Total | Rate | Caught by |
|---|---|---|---|---|
| Card testing | 77 | 80 | 0.96 | AmountOutlier (77) |
| Impossible travel | 11 | 22 | 0.50 | GeoImpossibility (11) |
| Burst | 47 | 90 | 0.52 | SpendVelocity (40), AmountOutlier (7) |
| Shopping trip *(legit)* | 2 | 204 | 0.01 | AmountOutlier (1), SpendVelocity (1) |
| Normal *(legit)* | 8 | 18,054 | 0.0004 | AmountOutlier (7), GeoImpossibility (1) |

Impossible travel sits at exactly 0.50 by construction: the pattern plants two
transactions, and the first has no meaningful prior to compare against, so only the
second can fire. This held at exactly 0.50 across all six dataset sizes tested
(4,687 to 147,950 transactions) — a structural ceiling, not a tuning failure.

Burst is the weakest at 0.52. The opening transactions of each burst pass before
enough history accumulates, so the engine catches the tail of an attack rather than
the head.

The false-positive rate on ordinary transactions is 0.04%. Precision alone hides how
rare that is.

### Rule contributions

| Rule | Fires on fraud | Fires on legitimate | Precision |
|---|---|---|---|
| AmountOutlier(4.0, min 5) | 84 | 8 | 0.91 |
| SpendVelocity(3, 3min, x6.0) | 40 | 1 | 0.98 |
| GeoImpossibility(900 km/h) | 11 | 1 | 0.92 |

**The rules are near-disjoint.** At 18,450 transactions no transaction is caught by
more than one rule. At 147,950, 9 of 1,435 caught transactions fire two rules
(SpendVelocity and AmountOutlier) — 0.6%. The clean partition at smaller sizes is a
property of the sample, not a guarantee.

Each rule targets a distinct signature: aggregate spend in a window, individual amount
deviation, and implied travel speed. A burst of four 3x transactions is 12x in
aggregate but each transaction sits below the 4x individual threshold, which is why
`SpendVelocity` and `AmountOutlier` rarely collide despite both reading amounts.

### Spend velocity: replacing count with spend

The original `VelocityRule` counted transactions in a window and ignored what they
were worth. Once the shopping-trip decoy was tightened to 45-second spacing, that rule
fired on 51 legitimate transactions against 45 fraudulent ones — precision 0.47.

Tempo could not separate the populations: bursts are 40s apart, trips 45s. Count
could, but only because trips contain exactly 4 transactions — tuning to that is
brittle and fails the moment a real shopper makes five purchases.

Amount was the robust separator. Bursts run 2–5x the account baseline, trips 0.4–1.4x,
and those distributions barely touch. `SpendVelocityRule` requires both a minimum
count in the window **and** aggregate window spend abnormal against the account's own
mean.

| Multiplier | Fraud caught | Shopping trips hit | Marginal cost |
|---|---|---|---|
| 8.0 | 35 | 0 | — |
| **6.0** | **40** | **1** | 5 TP per 1 FP |
| 4.0 | 45 | 12 | 5 TP per 11 FP |

The 8→6 step buys 5 true positives for 1 false positive. The 6→4 step buys the same 5
for 11. That inflection brackets the point where the spend distributions begin to
overlap. **6.0 is the operating point.**

At 4.0 the rule catches exactly 45 bursts — identical to the original count-based rule
— with 12 false positives against 51. Same detection, a quarter of the cost.

The fix was not a better threshold. It was a rule that stopped discarding information
it already held.

`VelocityRule` is retained in the codebase, marked superseded, so this comparison
remains verifiable rather than merely asserted.

### Amount outlier tuning

*Measured against an earlier generator configuration. Retained because the argument
holds; absolute figures do not correspond to the current dataset.*

| Rule config | Precision | Recall |
|---|---|---|
| AmountOutlier(3.0, min 5) | 0.77 | 0.61 |
| AmountOutlier(4.0, min 5) | 0.96 | 0.54 |
| AmountOutlier(6.0, min 5) | 0.99 | 0.49 |

- 6.0 → 4.0 bought 8 true positives for 2 false positives
- 4.0 → 3.0 bought 10 true positives for 23 false positives

The same shape as the spend velocity curve: a threshold below which fraud amounts stop
being separable from normal spending variance. Framed operationally, at 4x an analyst
receives 82 alerts of which 79 are real; at 3x, 115 of which 26 are wasted. Whether
that trade is worth making depends on the cost of a missed fraud against the cost of
an investigation — a business decision, not an engineering one. The engine's job is to
make the trade-off legible.

## Performance

The replay loop originally passed every rule a flat `List` of all prior transactions.
Each rule then filtered that list down to one account. With 800 accounts, that meant
scanning ~37,000 records to find the ~46 that mattered, three times per transaction —
O(n²), and worse in practice as memory pressure compounded.

Replacing the flat list with `AccountHistory`, a `Map<String, List<TransactionView>>`
keyed by account, turned "this account's history" from a scan into a lookup.

| Transactions | Before | After | Speedup |
|---|---|---|---|
| 4,687 | 633ms | 91ms | 7x |
| 9,352 | 2,780ms | 127ms | 22x |
| 18,450 | 8,920ms | 199ms | 45x |
| 36,935 | 68,546ms | 304ms | 225x |
| 74,033 | ~4.5 min* | 455ms | — |
| 147,950 | ~18 min* | 600ms | — |

*Extrapolated from the measured quadratic curve, not run. The original implementation
was not benchmarked at these sizes because the runtime made it impractical — which is
itself the point.*

Two further gains came from the structure rather than the map. Because the replay loop
feeds transactions chronologically, each account's list is inherently sorted, so
`since(accountId, cutoff)` walks backwards from the end and stops at the cutoff —
typically a handful of steps for a 3-minute window — and returns a `subList` view
rather than a copy. `GeoImpossibilityRule` previously streamed the full history with
`max(Comparator.comparing(...))` to find the most recent transaction; it is now a
single array index.

Every metric was identical before and after, down to individual rule firing counts,
across all six dataset sizes. The 22-test suite was what made that claim checkable
rather than a matter of eyeballing console output.

## REST API

*In progress on the `spring-api` branch.*

Spring Boot 3.4 wraps the engine. Two endpoints so far:

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Liveness check |
| GET | `/api/replay/default` | Replay with the shipped configuration |
| POST | `/api/replay` | Replay with caller-supplied rule parameters |

POST accepts every tunable as JSON — account count, dataset seed, and each rule's
thresholds — and returns metrics, per-rule firing counts, and per-pattern detection
rates. Reproducing a tuning run previously meant editing `Main`, recompiling, and
rerunning; it is now a field in a request body. This is the foundation a tuning UI
would sit on: each slider maps to one field.

Verified by reproducing the `SpendVelocity(x4.0)` result from the tuning table above
via the API — 45 fraud caught, 12 shopping trips hit, precision 0.87 — matching the
recompiled run exactly.

### API design decisions

- **The engine has no Spring dependencies.** `Rule`, `AccountHistory`,
  `TransactionView`, and the four rule implementations were not modified at all to
  add the API. Spring annotations appear only on classes in the web layer. The
  detection logic could sit behind a CLI, a Kafka consumer, or an HTTP endpoint
  without changing.
- **Consequently the 22 tests still run without an application context**, in well
  under a second. Annotating a rule class would end that.
- **`ReplayService` holds no state.** Spring creates one instance shared across all
  requests, so a mutable field would leak between callers. The confusion matrix,
  attribution report, and account history are all local variables constructed per
  call.
- **Parameters are a request object, not a long argument list.** `ReplayRequest`
  carries nine tunables with a `defaults()` factory holding the shipped
  configuration in one place, referenced by both the console harness and the API.
- **`ConfusionMatrix` and `AttributionReport` gained getters but return no internal
  collections.** Handing out a mutable map would let a caller corrupt the counts —
  the same instinct as the unmodifiable views in `AccountHistory`.
- **Records serialise directly.** `ReplayResult` and its nested `RuleStat` and
  `PatternStat` are the API response shape; Jackson maps them without any
  intermediate DTO layer.

### Not yet done
- No input validation. A request with a negative multiplier or an implausible
  account count is accepted and either produces nonsense or exhausts memory.
- No persistence. Replay results are computed and discarded, so configurations
  cannot be compared across sessions.
- No authentication.

## Corrections

Four claims in earlier versions of this document were wrong. They are recorded rather
than removed, because the corrections are the more useful content.

**Geo-impossibility precision was an artifact.** The rule initially scored 37 true
positives with 2 false positives and appeared to catch burst transactions as a bonus.
It did not. Positional jitter was applied per transaction, so six burst transactions
40 seconds apart sat up to 20km from each other — implying impossible speeds. Holding
location fixed within a burst dropped the rule to 11 true positives and 1 false
positive: exactly the pattern it was designed for, and nothing else. The apparent
bonus detection was the generator, not the rule.

**A hypothesis about rule overlap was half right.** With three rules, 18 transactions
fired more than one. The proposed explanation was that geo overlapped with velocity on
bursts. Attribution showed 10 velocity+geo and 8 amount+geo — and the reasoning that
had specifically ruled out amount+geo was wrong about which transactions were
involved. Both groups were bursts, split by whether velocity had accumulated enough
priors. Once the location artifact was fixed, all 18 overlaps disappeared.

**The shopping-trip decoy tested nothing for several runs.** It was spaced 2 minutes
apart against a 3-minute window, so at most one prior ever fell inside — no rule could
fire on it. Reported precision of 0.99 was measured against a decoy population that
was structurally invisible. Tightening to 45 seconds dropped precision to 0.65 and
exposed the count-based velocity rule's real false-positive rate.

**"Perfectly disjoint" was a small-sample claim.** Zero overlap held at 18,450
transactions and was stated as a property of the rules. At 147,950 transactions, 9
overlaps appear. The rules are near-disjoint, not disjoint.

The common thread: **every suspiciously good result in this project turned out to be a
property of the test data rather than the detection logic.** Precision above 0.95 was,
each time, a signal to inspect the generator.

## Design notes
- Rules sit behind a `Rule` interface with parameters injected via constructor. Adding
  the third and fourth rules required no change to the replay loop, the confusion
  matrix, the attribution report, or any other rule — one more entry in a list.
- **Rules cannot read the fraud label — enforced by the type system, not by
  convention.** `Transaction` carries `isFraud` and `fraudPattern`; rules receive a
  `TransactionView`, which has neither. The replay loop strips the labels before rules
  see the data and reads them only when scoring. A rule that peeks at the label scores
  perfectly and proves nothing, and the failure is invisible because the output looks
  excellent. Making it a compile error removes the possibility.
- The replay loop appends each transaction to history *after* evaluating it, so no
  rule can see the future. Reversing those two lines would inflate every metric here.
- The loop evaluates every rule on every transaction rather than short-circuiting.
  Wasted work for a boolean verdict, but a precondition for the attribution that
  produced most of the findings above.
- `AccountHistory` returns unmodifiable views. Rules receive history and must not
  mutate it.
- `SpendVelocityRule` excludes the transaction under judgment from the account
  baseline while including it in the window total. Folding it into its own baseline
  would dilute the signal being tested.
- `AmountOutlierRule` uses a per-account baseline. A global average would judge a
  customer who normally spends £15 and one who normally spends £200 by the same
  yardstick.
- Amount deviation is checked in both directions. Instinct says fraud means large
  amounts, but card testing is anomalous on the *low* side.
- `GeoImpossibilityRule` compares against the immediately previous transaction, not a
  window. Implied speed is meaningful only between consecutive events. It uses
  haversine rather than flat-plane distance — at Canadian latitudes a degree of
  longitude is roughly 75km against 111km for latitude.
- The geo rule guards against zero elapsed time. Identical timestamps would divide by
  zero, produce `Infinity`, and flag every simultaneous pair.
- `BigDecimal` for storing and summing money, converted to `double` for computing
  means. Rounding error accumulates when summing balances; it is irrelevant when
  asking whether a value is roughly 4x an average.
- Timestamps are `Instant` in UTC. Generated transactions are sorted chronologically
  before replay.

## Tests

22 unit tests across all four rules, running in well under a second. The rules have no
framework dependencies — plain Java over an in-memory structure — so tests need no
application context.

What they pin:

- **Threshold boundaries from both sides**, so the suite cannot pass against a rule
  that flags everything.
- **Account scoping on every rule.** Since the `AccountHistory` refactor this is
  partly guaranteed by the structure, but a wrong `accountId` would still break it.
- **Bidirectional amount deviation.** An implementation checking only the upper bound
  fails, and it is the lower bound that catches card testing.
- **Minimum-history and minimum-count guards**, pinning deliberate design decisions
  rather than observed behaviour.
- **Geo compares against the most recent prior, not the oldest.** An account that
  travels Vancouver→Toronto overnight then moves across Toronto is legitimate against
  the recent prior and impossible against the oldest.
- **The zero-elapsed-time guard.**
- **Spend velocity's core justification:** two tests with identical tempo — four
  transactions ~40s apart — where one is 3x baseline and one is 0.9x. One fires, one
  does not. The original count-based rule fires on both.

Each test isolates one failure mode, so a red test names what broke. The suite was
verified by deliberately removing an account filter and confirming exactly one test
failed.

It has since caught two refactors' worth of regressions in advance: the
`TransactionView` change and the `AccountHistory` rewrite both touched the interface,
every rule, the replay loop, and every test class, and in both cases combined metrics
came out identical afterwards.

## Known limitations
- **`AccountHistory.since` assumes chronological insertion order** and walks backwards
  from the end of each account's list. The replay loop guarantees this, but nothing
  enforces it. This surfaced while updating the tests: a baseline built in descending
  order silently read the wrong end. The optimisation traded a rule that filtered
  defensively for one that trusts its input — faster, and more fragile.
- Burst recall is 0.52. The opening transactions of each burst pass before enough
  history accumulates, and no current rule addresses the head of an attack.
- The replay loop, confusion matrix, attribution report, data generator, and the
  entire web layer are untested. Coverage is limited to rule logic.
- Legitimate behaviour is modelled by two patterns. Real traffic contains checkout
  retries, split payments, and subscription batches that this dataset does not model,
  so precision would degrade against production data.
- Merchant-category anomalies are unimplemented. Scope was cut at four rules
  deliberately: a fifth rule targeting a fifth planted pattern would not have changed
  any conclusion here.
- Alert severity is binary. Real systems score confidence and route accordingly.
- `AccountHistory` holds every transaction in memory. Fine for a bounded replay,
  unsuitable for an unbounded stream — a production version would evict outside the
  widest rule window.

## Stack
Java 21, Maven, JUnit 5, Spring Boot 3.4.

## Running

**Console harness:** run `com.hassan.anomaly.Main`. Adjust the `accounts` constant at
the top of `main` to change dataset size.

**API:** run `com.hassan.anomaly.AnomalyApplication`, then
`GET http://localhost:8080/api/replay/default`.

Tests run via `mvn test` or through the IDE.