import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { fetchHistory } from "../api";

const sameDataset = (a, b) =>
  a.seed === b.seed && a.accounts === b.accounts && a.days === b.days;

const metrics = [
  ["Precision (%)", "precision", 100],
  ["Recall (%)", "recall", 100],
  ["Fraud caught", "truePositives", 1],
  ["False alarms", "falsePositives", 1],
  ["Spend multiplier", "velocitySpendMultiplier", 1],
  ["Minimum burst count", "velocityMinCount", 1],
  ["Spend window (min)", "velocityWindowMinutes", 1],
  ["Amount multiplier", "amountMultiplier", 1],
  ["Minimum amount history", "amountMinHistory", 1],
  ["Travel threshold (km/h)", "geoMaxSpeedKmh", 1],
];

function CompareRuns({ runs }) {
  const [baselineId, setBaselineId] = useState(String(runs[0].id));
  const [candidateId, setCandidateId] = useState("");
  const baseline = runs.find((run) => String(run.id) === baselineId);
  const candidates = runs.filter((run) => run.id !== baseline.id && sameDataset(run, baseline));
  const candidate = candidates.find((run) => String(run.id) === candidateId) ?? candidates[0];
  const label = (run) => "#" + run.id + " · " + new Date(run.runAt).toLocaleString() + " · seed " + run.seed;
  const format = (value) => value.toLocaleString(undefined, { maximumFractionDigits: 3 });

  return (
    <section className="card">
      <h2>Compare rule settings</h2>
      <p className="muted">Compare replays with the same seed, account count, and number of days.</p>
      <div className="comparison-controls">
        <label>
          Baseline
          <select value={baselineId} onChange={(event) => {
            setBaselineId(event.target.value);
            setCandidateId("");
          }}>
            {runs.map((run) => <option key={run.id} value={run.id}>{label(run)}</option>)}
          </select>
        </label>
        <label>
          Compare with
          <select disabled={!candidate} value={candidate ? String(candidate.id) : ""}
            onChange={(event) => setCandidateId(event.target.value)}>
            {!candidate && <option value="">No matching replay</option>}
            {candidates.map((run) => <option key={run.id} value={run.id}>{label(run)}</option>)}
          </select>
        </label>
      </div>
      {!candidate && <p className="muted">Change a rule setting and <Link to="/run">run another test</Link> with the same dataset settings.</p>}
      {candidate && (
        <div className="table-scroll">
          <table>
            <thead><tr><th scope="col">Metric or setting</th><th scope="col">Baseline</th>
              <th scope="col">Comparison</th><th scope="col">Change</th></tr></thead>
            <tbody>
              {metrics.map(([title, field, scale]) => {
                const delta = Number(((candidate[field] - baseline[field]) * scale).toFixed(3));
                return <tr key={field}><th scope="row">{title}</th>
                  <td>{format(baseline[field] * scale)}</td>
                  <td>{format(candidate[field] * scale)}</td>
                  <td>{delta > 0 ? "+" : ""}{format(delta)}{scale === 100 ? " pp" : ""}</td></tr>;
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default function History() {
  const [runs, setRuns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        setRuns(await fetchHistory());
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const best = runs.length
    ? runs.reduce((a, b) => (b.precision > a.precision ? b : a))
    : null;

  return (
    <div className="page">
      <div className="page-head">
        <h1>History</h1>
        <p className="page-sub">
          Every test you have run is stored with its settings and results, so
          configurations can be compared rather than remembered.
        </p>
      </div>

      {loading && <div className="card"><div className="empty"><p>Loading…</p></div></div>}
      {error && <div className="error">{error}</div>}

      {!loading && !error && runs.length === 0 && (
        <div className="card">
          <div className="empty">
            <h3>Nothing here yet</h3>
            <p>Run a test and it will appear here.</p>
            <Link className="btn" to="/run">Run a test</Link>
          </div>
        </div>
      )}

      {runs.length > 0 && (
        <>
          <div className="metrics">
            <Metric label="Tests run" value={runs.length} />
            <Metric label="Best precision" value={best.precision.toFixed(3)}
                    note={`at spend ×${best.velocitySpendMultiplier}`} />
            <Metric label="Latest recall" value={runs[0].recall.toFixed(3)} />
            <Metric label="Transactions scanned"
                    value={runs.reduce((s, r) => s + r.transactions, 0).toLocaleString()} />
          </div>

          <CompareRuns runs={runs} />
          <section className="card">
            <h2>All runs</h2>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>When</th>
                    <th>Seed</th>
                    <th>Accounts</th>
                    <th>Spend ×</th>
                    <th>Amount ×</th>
                    <th>Geo km/h</th>
                    <th>Precision</th>
                    <th>Recall</th>
                    <th>Caught</th>
                    <th>False alarms</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {runs.map((r) => (
                    <tr key={r.id}>
                      <td className="muted">
                        {new Date(r.runAt).toLocaleString(undefined, {
                          month: "short", day: "numeric",
                          hour: "2-digit", minute: "2-digit",
                        })}
                      </td>
                      <td>{r.seed}</td>
                      <td>{r.accounts}</td>
                      <td>{r.velocitySpendMultiplier}</td>
                      <td>{r.amountMultiplier}</td>
                      <td>{r.geoMaxSpeedKmh}</td>
                      <td><strong>{r.precision.toFixed(3)}</strong></td>
                      <td><strong>{r.recall.toFixed(3)}</strong></td>
                      <td>{r.truePositives}</td>
                      <td>{r.falsePositives}</td>
                      <td className="muted">{r.replayTimeMs} ms</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
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
