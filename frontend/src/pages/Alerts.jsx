import { Link } from "react-router-dom";

export default function Alerts({ lastResult }) {
  return (
    <div className="page">
      <div className="page-head">
        <h1>Alerts</h1>
        <p className="page-sub">
          Accounts flagged by the rules, with the transactions that triggered each flag.
        </p>
      </div>

      {!lastResult ? (
        <div className="card">
          <div className="empty">
            <h3>No test run yet</h3>
            <p>Alerts appear here after a test.</p>
            <Link className="btn" to="/run">Run a test</Link>
          </div>
        </div>
      ) : (
        <div className="card">
          <div className="empty">
            <h3>Coming next</h3>
            <p>
              The last run flagged{" "}
              <strong>{lastResult.truePositives + lastResult.falsePositives}</strong>{" "}
              transactions. This page will group them by account, show the evidence
              behind each flag, and let you mark accounts as reviewed.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}