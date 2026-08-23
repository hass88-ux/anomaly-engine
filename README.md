# Transaction Anomaly Detection Engine

A rule-based fraud detection engine evaluated against synthetic transaction data
with planted fraud patterns, so precision and recall can be measured directly
rather than estimated.

Three rules with largely complementary blind spots reach recall 0.87 at precision
0.99. No individual rule exceeds 0.63 recall alone.

## Status
In progress. Engine, metrics, data generator, three rule types, combined
evaluation, and unit tests implemented. Per-rule attribution, a fourth rule type,
a REST API, and a tuning UI to follow.

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
sequence of random draws, every figure in this README comes from the current
generator configuration — earlier results were regenerated rather than carried
forward.

## Dataset design
Four patterns are planted, chosen so the results are not circular:

| Pattern | Label | Shape | Purpose |
|---|---|---|---|
| Burst | fraud | 6 txns, 40s apart, 2–5x normal amount | Fraud the velocity rule should catch |
| Card testing | fraud | 8 txns, 11h apart, small amounts | Invisible to velocity by design |
| Impossible travel | fraud | 2 txns, 25min apart, different cities | Invisible to velocity and amount by design |
| Shopping trip | legitimate | 4 txns, 2min apart, normal amounts | Legitimate behaviour that resembles fraud |

Each account is assigned a home city from five Canadian metros, and normal
transactions cluster within roughly 10km of it. Without a home location, accounts
would be scattered nationwide and impossible travel would be indistinguishable
from ordinary behaviour.

Card testing and impossible travel exist so no single rule can reach high recall —
a project reporting strong results from one rule is measuring its own assumptions.
Shopping trips exist so precision is contested: every false positive is a real
customer declined at a till.

## Results

18,457 transactions, 230 fraudulent (1.25%), seed 42.

### Velocity rule

Counts prior transactions from the same account within a time window.

| Rule config | Precision | Recall | TP | FP | FN |
|---|---|---|---|---|---|
| Velocity(3, 10min) | 0.54 | 0.23 | 33 | 28 | 113 |
| Velocity(4, 10min) | 1.00 | 0.15 | 22 | 0 | 124 |
| Velocity(3, 3min) | 1.00 | 0.23 | 33 | 0 | 113 |
| Velocity(2, 2min) | 1.00 | 0.30 | 44 | 0 | 102 |

*Figures above predate the geo pattern and a smaller dataset; they are retained for
the tuning argument rather than as current numbers.*

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
`isAfter` comparison excludes transactions sitting precisely on the cutoff. Config
3 excludes decoys by a full minute of margin, so it is the more defensible
configuration — a rule whose accuracy depends on a tie-break is not one to deploy.

**Why velocity recall is capped.** It requires N priors before firing, so the
opening transactions of every burst pass silently: it catches the tail of an
attack, never the head. Card testing and impossible travel are invisible to it
entirely.

### Amount outlier rule

Compares a transaction against the mean of that account's own prior transactions,
flagging deviation in **either** direction. Silent until the account has a minimum
number of priors, since a baseline computed from two data points is not a baseline.

| Rule config | Precision | Recall | TP | FP | FN |
|---|---|---|---|---|---|
| AmountOutlier(3.0, min 5) | 0.77 | 0.61 | 89 | 26 | 57 |
| AmountOutlier(4.0, min 5) | **0.96** | **0.54** | 79 | 3 | 67 |
| AmountOutlier(6.0, min 5) | 0.99 | 0.49 | 71 | 1 | 75 |

*Also measured against the earlier dataset.*

**Reading these.** The exchange rate between thresholds is where the useful finding
is:

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

### Geo-impossibility rule

Compares a transaction against the account's immediately preceding transaction and
flags when implied travel speed exceeds a threshold. Distance uses the haversine
formula rather than treating lat/lon as a flat plane — at Canadian latitudes a
degree of longitude is roughly 75km against 111km for latitude, so the flat
approximation would be materially wrong.

| Rule config | Precision | Recall | TP | FP | FN |
|---|---|---|---|---|---|
| GeoImpossibility(900 km/h) | 0.95 | 0.16 | 37 | 2 | 193 |

900 km/h is approximately airliner cruising speed: anything faster implies the two
transactions were not made by the same person.

Unlike the other two rules, this one uses only the **immediately previous**
transaction rather than a window. Implied speed is meaningful only between
consecutive events; comparing against a transaction from three days ago yields a
trivially achievable speed and the rule would never fire.

Recall is low by design — this rule targets one narrow attack shape and catches
roughly half of it, since the first transaction of each planted pair has no
meaningful prior to compare against. A rule that detects one thing well is more
useful in combination than a broad rule that detects everything poorly.

**The 2 false positives are instructive.** Normal transactions never span cities,
so the flagged pairs are ordinary transactions that happened to land a minute or
two apart within the same metro. At one minute of separation, even 15km of
positional jitter implies 900 km/h. The rule is arithmetically correct; the data
contains a legitimate case that looks impossible. This mirrors a real problem —
card-present and card-not-present transactions in the same city minutes apart are
routine, and naive geo rules fire on them constantly.

### Combined rules

All three evaluated together, flagging a transaction if **any** fires.

| Configuration | Precision | Recall | TP | FP | FN |
|---|---|---|---|---|---|
| Velocity + Amount | 1.00 | 0.78 | 180 | 0 | 50 |
| GeoImpossibility alone | 0.95 | 0.16 | 37 | 2 | 193 |
| **All three** | **0.99** | **0.87** | 199 | 2 | 31 |

**The rules are not additive.** 180 + 37 = 217, but the combination yields 199 —
so 18 transactions are caught by more than one rule, and geo contributes 19 net
rather than 37.

The likely source is the burst pattern. Bursts place 6 transactions 40 seconds
apart, each with independent positional jitter of up to ~20km. At 40-second gaps
that implies speeds far above the threshold, so geo fires on transactions velocity
and amount already caught. This is an artifact of the generator jittering location
per transaction: a real card-present burst at a single merchant would share one
location.

An earlier two-rule combination was perfectly disjoint (33 + 79 = 112 exactly).
That was a property of that specific pair rather than a general law, and adding a
third rule produced measurable redundancy. Coverage is best described as *largely
complementary with quantifiable overlap*, not as a clean partition.

**This claim is currently inferred, not measured.** Alerts do not record which rule
raised them, so the burst attribution above is a hypothesis consistent with the
numbers rather than a demonstrated fact. Per-rule attribution is the next planned
change, and it exists specifically to test this.

**Why OR rather than AND.** AND-combination would flag only transactions all rules
agree on — close to none. Velocity cannot see card testing, amount cannot see
sub-4x bursts, geo cannot see anything within one city. Requiring consensus would
collapse recall. OR is correct when rules have complementary blind spots; it would
be the wrong choice for heavily overlapping rules, where AND would suppress shared
false positives.

## Design notes
- Rules sit behind a `Rule` interface with parameters injected via constructor, so
  one implementation runs under many configurations. Every result above required no
  code changes beyond constructor arguments, and adding the third rule required no
  changes to the other two or to the replay loop — only one more entry in a list.
- **Rules cannot read the fraud label — enforced by the type system, not by
  convention.** `Transaction` carries `isFraud`; rules receive a `TransactionView`,
  which does not have the field. The replay loop strips the label before rules see
  the data and reads it only when scoring. Discipline alone would not be enough: a
  rule that peeks at the label scores perfectly and proves nothing, and the failure
  is invisible because the output looks excellent. Making it a compile error removes
  the possibility rather than warning against it.
- The replay loop appends each transaction to history *after* evaluating it, so no
  rule can see the future. Reversing those two lines would inflate every metric in
  this README.
- The replay loop evaluates every rule on every transaction rather than
  short-circuiting once one fires. This is wasted work for a boolean verdict, but
  it is a precondition for per-rule attribution.
- The amount rule uses a **per-account** baseline rather than a global one. A global
  average would judge a customer who normally spends £15 and one who normally spends
  £200 by the same yardstick.
- Amount deviation is checked in both directions. Instinct says fraud means large
  amounts, but card testing is anomalous on the *low* side — small probe
  transactions verifying a stolen card is live.
- The amount rule stays silent below a minimum history threshold. A rule firing
  confidently on two data points is detecting sparse data, not anomalies.
- The geo rule guards against zero elapsed time. Two transactions with identical
  timestamps would divide by zero and produce `Infinity`, which exceeds any
  threshold — every simultaneous pair would flag.
- `BigDecimal` is used for storing and summing money, but converted to `double` for
  computing means. Rounding error accumulates when summing balances; it is
  irrelevant when asking whether a value is roughly 4x an average.
- Timestamps are `Instant` in UTC throughout.
- Generated transactions are sorted chronologically before replay — accounts are
  built one at a time, so unsorted data would place future events in a transaction's
  history.

## Tests

Nine unit tests across the velocity and amount rules, running in ~50ms. Speed comes
from the rules having no framework dependencies — plain Java operating on lists, so
tests need no application context.

The tests pin behaviour that would otherwise silently change:

- **Threshold boundaries from both sides.** Velocity fires at 3 priors and stays
  silent at 2. A test checking only the firing case would pass against a rule that
  flags everything.
- **The account filter**, on both rules. Removing it turns velocity into a global
  transaction counter and gives the amount rule a global baseline — both would still
  run, produce plausible numbers, and be wrong.
- **The time window.** Without it, velocity counts an account's entire history.
- **Bidirectional deviation.** Both large- and small-amount cases are asserted, so
  an implementation checking only the upper bound fails — and it is the lower bound
  that catches card testing.
- **The minimum-history guard.** A £5,000 transaction against a £50 baseline is
  asserted *not* to fire when only 3 priors exist, pinning a deliberate design
  decision rather than an observed behaviour.

Each test isolates one failure mode, so a red test names what broke rather than only
that something did. The suite was verified by deliberately removing the account
filter and confirming exactly one test failed.

The `TransactionView` refactor was validated against this suite: the type change
touched the `Rule` interface, both rule implementations, the replay loop, and both
test classes, and combined metrics came out identical afterwards. A refactor that
changes behaviour is not a refactor, and without the suite that claim would rest on
eyeballing console output.

## Known limitations
- Alerts are not attributed to the rule that raised them, so the overlap analysis
  above is inferred rather than measured.
- The geo rule is untested. Its haversine implementation and zero-elapsed-time guard
  have no unit coverage.
- Rule evaluation is O(n²): full history is scanned for every transaction, for every
  rule. Acceptable at ~18k transactions, not beyond. A per-account sliding window
  with eviction is the fix.
- Test coverage is limited to rule logic. The replay loop, confusion matrix, and
  data generator are untested.
- Positional jitter is applied per transaction, including within bursts. A real
  card-present burst at one merchant would share a location, so the geo rule's
  overlap with velocity is partly a generator artifact.
- Legitimate burst behaviour is modelled by a single fixed pattern, which overstates
  velocity precision.
- Merchant-category anomalies are unimplemented.

## Stack
Java 21, Maven. Spring Boot, MySQL, and a React tuning UI planned.

## Running
Clone, import as a Maven project, run `com.hassan.anomaly.Main`. Tests run via
`mvn test` or through the IDE.