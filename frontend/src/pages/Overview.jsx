import { Link } from "react-router-dom";

export default function Overview({ username, lastResult }) {
  return (
    <div className="page">
      <div className="page-head">
        <h1>Welcome back, {username}</h1>
        <p className="page-sub">
          This tool replays synthetic card transactions through fraud detection rules
          and measures how well they perform.
        </p>
      </div>

      {!lastResult ? (
        <div className="card intro">
          <h2>Start here</h2>
          <ol className="steps">
            <li>
              <strong>Run a test.</strong> Generates a dataset of transactions with
              fraud deliberately planted, then runs the rules against it.
            </li>
            <li>
              <strong>Read the results.</strong> Because the fraud is planted, the tool
              knows exactly what it should have caught — so accuracy is measured, not
              estimated.
            </li>
            <li>
              <strong>Tune and compare.</strong> Adjust a rule, run again, and see what
              you gained and what it cost.
            </li>
          </ol>
          <div className="cta">
            <Link className="btn" to="/run">Run your first test</Link>
            <Link className="btn-quiet" to="/manual">Read the manual</Link>
          </div>
        </div>
      ) : (
        <>
          <div className="card">
            <div className="card-head">
              <h2>Last test</h2>
              <Link className="btn-quiet" to="/run">Run another</Link>
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