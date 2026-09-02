import { Link } from "react-router-dom";
import Logo from "../Logo";
import "../landing.css";

export default function Guide() {
  return (
    <div className="landing">
      <header className="landing-bar">
        <Link to="/" className="brand">
          <Logo size={28} />
          <span>Anomaly Engine</span>
        </Link>
        <nav>
          <a href="https://github.com/hass88-ux/anomaly-engine">Source</a>
          <Link className="btn-primary" to="/login">Sign in</Link>
        </nav>
      </header>

      <div className="guide">
        <div className="guide-head">
          <h1>How it works</h1>
          <p className="lede">
            A walkthrough of what this tool does, how to use it, and how to read what it
            gives you back. Roughly a five minute read.
          </p>
        </div>

        <nav className="guide-toc">
          <a href="#problem">The problem</a>
          <a href="#two-modes">Two ways in</a>
          <a href="#upload">Uploading a file</a>
          <a href="#results">Reading the results</a>
          <a href="#queue">Working the alert queue</a>
          <a href="#rules">The three rules</a>
          <a href="#generated">Generated data</a>
          <a href="#limits">What it does not do</a>
        </nav>

        <section id="problem" className="guide-section">
          <h2>The problem this solves</h2>
          <p>
            Fraud detection is not hard to build. Writing a rule that flags large
            transactions takes ten minutes. What is hard is knowing whether the rule is any
            good.
          </p>
          <p>
            In a real payments business you find out weeks later, when customers dispute
            charges. By then the rule has been running the whole time, either declining
            honest customers or letting fraud through, and nobody could tell which. Fraud
            teams change thresholds and hope.
          </p>
          <p>
            This tool addresses that in two ways. It can generate transaction data with
            fraud deliberately planted inside it, so accuracy is <em>measured</em> rather
            than estimated. And it can run those same calibrated rules against a file of
            your own, producing a queue of accounts worth investigating, with the evidence
            attached to each one.
          </p>
        </section>

        <section id="two-modes" className="guide-section">
          <h2>Two ways in</h2>

          <div className="guide-split">
            <div className="guide-mode">
              <h3>Upload your own CSV</h3>
              <p>
                The practical mode. Point the rules at real transactions and get back
                flagged accounts. No fraud label exists in real data, so the tool produces
                alerts but cannot score itself.
              </p>
            </div>
            <div className="guide-mode">
              <h3>Run on generated data</h3>
              <p>
                The measurement mode. Fraud is planted, so the tool knows exactly what it
                should have caught. This is the only way to tell whether changing a
                threshold helped or hurt.
              </p>
            </div>
          </div>

          <p>
            The intended order is: calibrate on generated data, then carry the thresholds
            you settled on to your own file. Skipping the first step means running rules
            whose behaviour you have never verified.
          </p>
        </section>

        <section id="upload" className="guide-section">
          <h2>Uploading a file</h2>

          <ol className="guide-steps">
            <li>
              <h3>Drop in a CSV</h3>
              <p>
                Up to 25 MB. Only three columns are required: something that identifies the
                account or card, a timestamp, and an amount. Everything else is optional.
              </p>
              <figure className="guide-shot">
                <img src="/guide/upload.png" alt="The upload page with an empty dropzone" />
                <figcaption>Drag a file in, or click to browse.</figcaption>
              </figure>
            </li>

            <li>
              <h3>Confirm the columns</h3>
              <p>
                Your header row is read and matched against known names —{" "}
                <code>customer_id</code>, <code>cardId</code>, <code>account</code> and{" "}
                <code>userId</code> all resolve to the account column. The guesses appear
                pre-filled in seven dropdowns, alongside the first five rows of your file.
              </p>
              <p>
                <strong>Check them against the sample rows.</strong> This step exists
                because a tool that silently analysed your data against the wrong amount
                column would produce output that looks entirely convincing and is entirely
                wrong. A guess you can correct is useful; a guess you cannot see is
                dangerous.
              </p>
              <figure className="guide-shot">
                <img
                  src="/guide/mapping.png"
                  alt="Seven dropdowns pre-filled with detected column names, above five sample rows"
                />
                <figcaption>
                  Detection has matched <code>customer_id</code> to Account and{" "}
                  <code>transaction_date</code> to Timestamp. The amber warning notes there
                  is no fraud label in this file, so the run cannot be scored.
                </figcaption>
              </figure>
            </li>

            <li>
              <h3>Read the warnings</h3>
              <p>
                If your file has no coordinates, the impossible-travel rule is skipped
                rather than fed zeros. If it has no fraud label, alerts are still produced
                but precision and recall cannot be computed. You are told both before
                anything runs, not after.
              </p>
            </li>

            <li>
              <h3>Adjust the rule thresholds, or leave them</h3>
              <p>
                The defaults are the configuration calibrated on generated data. They are a
                reasonable starting point, not a universal answer — see{" "}
                <a href="#limits">what it does not do</a>.
              </p>
            </li>

            <li>
              <h3>Analyse</h3>
              <p>
                The file is processed in the background and progress is reported as it
                goes. Large files are not analysed inside the web request, because a
                two-million-row file would time out and the work would be lost with no
                record it ever happened.
              </p>
              <p>
                Your file is read as a stream rather than loaded into memory, so a
                five-million-row file uses the same memory as a fifty-thousand-row one.
                Once analysis finishes, the file is deleted.
              </p>
            </li>
          </ol>

          <div className="guide-aside">
            <h3>What happens to messy data</h3>
            <p>
              Real exports are not clean, and most of the ingestion code exists to deal
              with that. Currency symbols and thousands separators are stripped.
              Accounting-style parentheses are read as negatives. Eleven date formats are
              tried, along with Unix timestamps in seconds and milliseconds. Both{" "}
              <code>1,234.56</code> and <code>1.234,56</code> are read correctly — whichever
              separator appears last is treated as the decimal point.
            </p>
            <p>
              Rows that cannot be read are counted, listed by line number with the specific
              problem, and skipped. Three broken rows in a twenty-thousand-row file cost
              you three rows, not the file.
            </p>
          </div>
        </section>

        <section id="results" className="guide-section">
          <h2>Reading the results</h2>

          <p>
            Four numbers come back. Two are counts and two are only meaningful if your file
            had a fraud label.
          </p>

          <dl className="guide-defs">
            <div>
              <dt>Rows analysed</dt>
              <dd>
                How many transactions were successfully parsed, and how many were rejected.
              </dd>
            </div>
            <div>
              <dt>Flagged accounts</dt>
              <dd>
                Distinct accounts with at least one flagged transaction. This is the size of
                your review queue.
              </dd>
            </div>
            <div>
              <dt>Precision</dt>
              <dd>
                Of everything flagged, how much was actually fraud. Low precision means
                honest customers declined at the till and analysts working alerts that go
                nowhere.
              </dd>
            </div>
            <div>
              <dt>Recall</dt>
              <dd>
                Of all the fraud present, how much was caught. Low recall means money
                walking out the door.
              </dd>
            </div>
          </dl>

          <p>
            <strong>Precision and recall pull against each other.</strong> Loosen a rule and
            you catch more fraud but annoy more customers. Tighten it and the reverse. No
            setting maximises both. Where you sit on that curve is a business decision about
            what a missed fraud costs versus what an investigation costs — the tool's job is
            to make the trade-off visible, not to choose for you.
          </p>

          <p>
            If your file had no fraud label, both show a dash rather than zero. Zero would
            be a claim about accuracy; a dash is the truth, which is that accuracy is
            undefined without ground truth to compare against.
          </p>

          <figure className="guide-shot">
            <img
              src="/guide/results.png"
              alt="Four metric tiles and a table of three unreadable rows"
            />
            <figcaption>
              20,192 rows analysed and three rejected, each listed by line number with the
              specific problem. Precision and recall show a dash because this file had no
              fraud label.
            </figcaption>
          </figure>
        </section>

        <section id="queue" className="guide-section">
          <h2>Working the alert queue</h2>

          <p>
            Flagged transactions are grouped by account, because an analyst investigates an
            account rather than a transaction. Each account gets a confidence tier:
          </p>

          <dl className="guide-defs">
            <div>
              <dt><span className="badge high">HIGH</span></dt>
              <dd>
                Two or more distinct rules fired, or several transactions were flagged. The
                rules detect different things, so agreement between them means something.
              </dd>
            </div>
            <div>
              <dt><span className="badge medium">MEDIUM</span></dt>
              <dd>A repeated pattern on one rule.</dd>
            </div>
            <div>
              <dt><span className="badge low">LOW</span></dt>
              <dd>A single flag. Could be noise.</dd>
            </div>
          </dl>

          <p>
            Expanding an account shows every flagged transaction with its time, amount,
            nearest city, and which rules fired. That is the evidence — you are never asked
            to trust a score without seeing what produced it.
          </p>

          <figure className="guide-shot">
            <img
              src="/guide/alerts.png"
              alt="A list of flagged accounts with confidence badges and review controls"
            />
            <figcaption>
              Each account shows which rules fired, how many transactions were flagged, and
              the total amount involved. Filters narrow by confidence and by review status.
            </figcaption>
          </figure>

          <p>
            Each account can be marked <strong>reviewed</strong>,{" "}
            <strong>escalated</strong> or <strong>dismissed</strong>, with an optional note.
            Decisions are saved to your account and survive between sessions. Clicking a
            status you have already set clears it.
          </p>

          <p>
            A note attaches to a decision, so the note button stays disabled until you have
            made one — there is nowhere to store a note about an account you have not
            judged.
          </p>

          <div className="guide-aside">
            <h3>Keyboard shortcuts</h3>
            <p>
              The queue is built to be worked with the keyboard rather than the mouse.
              Press <kbd>?</kbd> on the alerts page for the full list.
            </p>
            <dl className="guide-keys">
              <div><dt><kbd>J</kbd> <kbd>K</kbd></dt><dd>Move between alerts</dd></div>
              <div><dt><kbd>Enter</kbd></dt><dd>Expand the evidence</dd></div>
              <div><dt><kbd>R</kbd></dt><dd>Mark reviewed</dd></div>
              <div><dt><kbd>E</kbd></dt><dd>Escalate</dd></div>
              <div><dt><kbd>D</kbd></dt><dd>Dismiss</dd></div>
              <div><dt><kbd>N</kbd></dt><dd>Add a note</dd></div>
            </dl>
          </div>
        </section>

        <section id="rules" className="guide-section">
          <h2>The three rules</h2>

          <div className="guide-rule">
            <h3>Spend velocity</h3>
            <p>
              Flags an account whose total spend across a short window is far above its own
              average, provided a minimum number of transactions occurred in that window.
              Catches a stolen card being drained quickly.
            </p>
            <p className="guide-note">
              Note it compares each account against <em>itself</em>. Someone who normally
              spends $15 and someone who normally spends $200 are judged by different
              standards, because a global average would flag the second customer constantly
              and never notice the first being defrauded.
            </p>
          </div>

          <div className="guide-rule">
            <h3>Amount outlier</h3>
            <p>
              Flags a single transaction far above <em>or below</em> the account's normal
              size, once enough history exists to establish what normal is.
            </p>
            <p className="guide-note">
              The lower bound matters as much as the upper. Card testing — validating a
              stolen card with $1 to $4 purchases before making a real one — is anomalous on
              the low side. This is why some alerts show a total of twenty dollars across
              eight transactions. That is the signal, not a rounding error.
            </p>
          </div>

          <div className="guide-rule">
            <h3>Geographic impossibility</h3>
            <p>
              Flags a card used in two places too far apart to travel between in the time
              elapsed. A purchase in Toronto and another in Vancouver 25 minutes later
              implies about 8,000 km/h, so one of them is not the cardholder.
            </p>
            <p className="guide-note">
              Compares against the immediately previous transaction rather than a window,
              because implied speed is only meaningful between consecutive events. Skipped
              entirely when a file has no coordinates.
            </p>
          </div>
        </section>

        <section id="generated" className="guide-section">
          <h2>Generated data, and why it exists</h2>

          <p>
            The generator produces transactions for a configurable number of accounts across
            a configurable number of days, with four patterns planted inside:
          </p>

          <div className="table-scroll">
            <table>
              <thead>
                <tr><th>Pattern</th><th>Shape</th><th>Represents</th></tr>
              </thead>
              <tbody>
                <tr>
                  <td><strong>Burst</strong></td>
                  <td>Six transactions 40 seconds apart, several times the usual amount</td>
                  <td>A stolen card being drained</td>
                </tr>
                <tr>
                  <td><strong>Card testing</strong></td>
                  <td>Eight tiny transactions, 11 hours apart</td>
                  <td>Checking a stolen card still works</td>
                </tr>
                <tr>
                  <td><strong>Impossible travel</strong></td>
                  <td>Two transactions 25 minutes apart in different cities</td>
                  <td>The card being used in two places at once</td>
                </tr>
                <tr>
                  <td>
                    <strong>Shopping trip</strong>{" "}
                    <span className="tag">legitimate</span>
                  </td>
                  <td>Four transactions 45 seconds apart, ordinary amounts</td>
                  <td>An honest customer who looks guilty</td>
                </tr>
              </tbody>
            </table>
          </div>

          <p>
            That last pattern is the important one. Without legitimate behaviour that
            closely resembles fraud, precision would be trivially perfect and the numbers
            would prove nothing. It exists specifically to make the tool's job hard.
          </p>

          <p>
            The dataset is seeded, so the same seed always produces identical data. Without
            that, a metric moving between runs could mean an improved rule or just a
            different random draw. Changing the seed and re-running is how you check whether
            a finding is real: anything that disappears under a different seed never was.
          </p>

          <figure className="guide-shot">
            <img
              src="/guide/history.png"
              alt="A table of past runs with their settings and resulting metrics"
            />
            <figcaption>
              Every run is stored with its configuration, so two settings can be compared
              rather than remembered. Here a max speed of 100 km/h caught far more fraud —
              and produced 2,666 false alarms doing it.
            </figcaption>
          </figure>
        </section>

        <section id="limits" className="guide-section">
          <h2>What it does not do</h2>

          <ul className="guide-limits">
            <li>
              <strong>It does not replace a trained model.</strong> On real labelled data at
              volume, a gradient-boosted model would very likely outperform three
              thresholds. Rules were chosen because every alert can state exactly why it
              fired, and because explainability matters when you are declining someone's
              card.
            </li>
            <li>
              <strong>Thresholds do not transfer automatically.</strong> The defaults assume
              intercontinental impossible travel. A file whose furthest hop is Toronto to
              Ottawa — about 530 km/h implied — will never trigger that rule at the default
              900 km/h. The rule is not broken; the configuration is wrong for that data.
              This is the most common way to get a misleadingly low recall.
            </li>
            <li>
              <strong>The generated data is synthetic.</strong> Real traffic contains
              checkout retries, split payments and subscription batches that it does not
              model, so precision measured on it is optimistic.
            </li>
            <li>
              <strong>Uploaded transactions are assumed to be in time order.</strong> A file
              sorted by account rather than by timestamp will produce wrong results rather
              than an error.
            </li>
            <li>
              <strong>Three rules cannot catch everything.</strong> Some planted fraud is
              invisible to all of them by design. A tool that scored perfectly would be
              measuring its own assumptions.
            </li>
          </ul>
        </section>

        <div className="guide-cta">
          <h2>Try it</h2>
          <p>
            The demo account has sample data already loaded, so you can look at a populated
            alert queue without uploading anything.
          </p>
          <div className="hero-actions">
            <Link className="btn-primary lg" to="/login">Open the app</Link>
            <a className="btn-ghost lg" href="https://github.com/hass88-ux/anomaly-engine">
              Read the code
            </a>
          </div>
        </div>
      </div>

      <footer className="landing-foot">
        <span>Built by Muhammad Hassan Amir</span>
        <a href="https://github.com/hass88-ux/anomaly-engine">github.com/hass88-ux</a>
      </footer>
    </div>
  );
}