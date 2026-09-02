import { Link } from "react-router-dom";

export default function Overview({ username, lastResult }) {
  return (
    <div className="page">
      <div className="page-head">
        <h1>Welcome back, {username}</h1>
        <p className="page-sub">
          Run fraud detection rules against your own transaction files, or against
          generated data where the answer is known so the rules can be measured.
        </p>
      </div>

      {!lastResult ? (
        <>
          <div className="two-col">
            <div className="card intro">
              <h2>Analyse your own data</h2>
              <p className="plain">
                Upload a CSV of transactions. Columns are detected automatically, values
                are coerced from whatever format they arrive in, and malformed rows are
                reported by line number rather than failing the file.
              </p>
              <p className="help" style={{ marginBottom: 18 }}>
                Needs an account identifier, a timestamp and an amount. Everything else is
                optional.
              </p>
              <Link className="btn" to="/upload">Upload a file</Link>
            </div>

            <div className="card intro">
              <h2>Or measure the rules first</h2>
              <p className="plain">
                Generated data has fraud deliberately planted inside it, so the tool knows
                exactly what it should have caught. That is the only way to know whether a
                threshold change helped or hurt.
              </p>
              <p className="help" style={{ marginBottom: 18 }}>
                Worth doing before pointing the rules at data you care about.
              </p>
              <Link className="btn-quiet" to="/run">Run a test</Link>
            </div>
          </div>

          <div className="card intro">
            <h2>How the two fit together</h2>
            <ol className="steps">
              <li>
                <strong>Calibrate on generated data.</strong> Precision and recall are
                computable because the ground truth is known.
              </li>
              <li>
                <strong>Carry those thresholds to your own file.</strong> Real exports have
                no fraud label, so alerts can be produced but not scored.
              </li>
              <li>
                <strong>Work the queue.</strong> Mark accounts reviewed, escalated or
                dismissed, with notes. Decisions are saved to your account.
              </li>
            </ol>
            <div className="cta">
              <Link className="btn-quiet" to="/manual">Read the manual</Link>
            </div>
          </div>
        </>
      ) : (
        <>
          <div className="card">
            <div className="card-head">
              <h2>Last test</h2>
              <div className="cta" style={{ marginTop: 0 }}>
                <Link className="btn-quiet" to="/run">Run another</Link>
                <Link className="btn-quiet" to="/upload">Upload a file</Link>
              </div>
            </div>

            <p className="plain">
              Of <strong>{lastResult.truePositives + lastResult.falsePositives}</strong>{" "}
              transactions flagged, <strong>{lastResult.truePositives}</strong> were
              genuinely fraudulent and{" "}
              <strong>{lastResult.falsePositives}</strong> were legitimate customers
              caught by mistake.{" "}
              <strong>{lastResult.falseNegatives}</strong> fraudulent transactions were
              missed.
            </p>

            <div className="metrics">
              <Metric label="Precision" value={lastResult.precision.toFixed(3)}
                      note="Share of flags that were real fraud" />
              <Metric label="Recall" value={lastResult.recall.toFixed(3)}
                      note="Share of real fraud that was caught" />
              <Metric label="Transactions" value={lastResult.transactions.toLocaleString()}
                      note="Scanned in this run" />
              <Metric label="Time" value={`${lastResult.replayTimeMs} ms`}
                      note="Full replay duration" />
            </div>
          </div>

          <div className="quick-links">
            <Link className="tile" to="/alerts">
              <h3>Alerts</h3>
              <p>Accounts flagged in this run, with the evidence behind each</p>
            </Link>
            <Link className="tile" to="/upload">
              <h3>Upload data</h3>
              <p>Point these rules at a CSV of your own transactions</p>
            </Link>
            <Link className="tile" to="/analysis">
              <h3>Analysis</h3>
              <p>Sweep a rule across thresholds and see the trade-off curve</p>
            </Link>
            <Link className="tile" to="/history">
              <h3>History</h3>
              <p>Every test you have run, side by side</p>
            </Link>
          </div>
        </>
      )}
    </div>
  );
}

function Metric({ label, value, note }) {
  return (
    <div className="metric">
      <div className="metric-label">{label}</div>
      <div className="metric-value">{value}</div>
      {note && <div className="metric-note">{note}</div>}
    </div>
  );
}