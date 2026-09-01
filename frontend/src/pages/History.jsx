import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { fetchHistory } from "../api";

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

      {!loading && runs.length === 0 && (
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