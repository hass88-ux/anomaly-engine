import { useState } from "react";
import { runReplay } from "./api";

const DEFAULTS = {
  accounts: 400,
  days: 30,
  seed: 42,
  velocityMinCount: 3,
  velocityWindowMinutes: 3,
  velocitySpendMultiplier: 6.0,
  amountMultiplier: 4.0,
  amountMinHistory: 5,
  geoMaxSpeedKmh: 900.0,
};

const CONTROLS = [
  { key: "velocitySpendMultiplier", label: "Spend velocity multiplier", min: 2, max: 12, step: 0.5,
    help: "Window spend must exceed this multiple of the account's average" },
  { key: "velocityMinCount", label: "Spend velocity min count", min: 2, max: 10, step: 1,
    help: "Transactions required in the window before the rule can fire" },
  { key: "velocityWindowMinutes", label: "Spend velocity window (min)", min: 1, max: 30, step: 1,
    help: "How far back the rule looks" },
  { key: "amountMultiplier", label: "Amount outlier multiplier", min: 2, max: 10, step: 0.5,
    help: "Deviation from the account's mean, in either direction" },
  { key: "amountMinHistory", label: "Amount outlier min history", min: 1, max: 20, step: 1,
    help: "Prior transactions needed before a baseline is trusted" },
  { key: "geoMaxSpeedKmh", label: "Geo max speed (km/h)", min: 100, max: 2000, step: 50,
    help: "Implied travel speed above which a transaction is impossible" },
];

export default function Sandbox({ username, onLogout }) {
  const [params, setParams] = useState(DEFAULTS);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function update(key, value) {
    setParams({ ...params, [key]: Number(value) });
  }

  async function run() {
    setBusy(true);
    setError("");
    try {
      setResult(await runReplay(params));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="app">
      <header>
        <h1>Anomaly Engine</h1>
        <div className="header-right">
          <span>{username}</span>
          <button className="link" onClick={onLogout}>Sign out</button>
        </div>
      </header>

      <div className="layout">
        <section className="panel">
          <h2>Rule parameters</h2>

          {CONTROLS.map((c) => (
            <div className="control" key={c.key}>
              <div className="control-head">
                <label>{c.label}</label>
                <span className="value">{params[c.key]}</span>
              </div>
              <input
                type="range"
                min={c.min}
                max={c.max}
                step={c.step}
                value={params[c.key]}
                onChange={(e) => update(c.key, e.target.value)}
              />
              <p className="help">{c.help}</p>
            </div>
          ))}

          <div className="actions">
            <button onClick={run} disabled={busy}>
              {busy ? "Running…" : "Run replay"}
            </button>
            <button className="link" onClick={() => setParams(DEFAULTS)}>
              Reset
            </button>
          </div>

          {error && <div className="error">{error}</div>}
        </section>

        <section className="results">
          {!result && !busy && (
            <div className="empty">Run a replay to see results</div>
          )}

          {result && (
            <>
              <div className="metrics">
                <Metric label="Precision" value={result.precision.toFixed(3)} />
                <Metric label="Recall" value={result.recall.toFixed(3)} />
                <Metric label="True positives" value={result.truePositives} />
                <Metric label="False positives" value={result.falsePositives} />
                <Metric label="Missed" value={result.falseNegatives} />
                <Metric label="Replay time" value={`${result.replayTimeMs} ms`} />
              </div>

              <h2>Detection by pattern</h2>
              <table>
                <thead>
                  <tr>
                    <th>Pattern</th><th>Caught</th><th>Total</th><th>Rate</th>
                  </tr>
                </thead>
                <tbody>
                  {result.patternStats.map((p) => (
                    <tr key={p.pattern}>
                      <td>{p.pattern}</td>
                      <td>{p.caught}</td>
                      <td>{p.total}</td>
                      <td>{(p.rate * 100).toFixed(1)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <h2>Rule contributions</h2>
              <table>
                <thead>
                  <tr>
                    <th>Rule</th><th>On fraud</th><th>On legitimate</th>
                  </tr>
                </thead>
                <tbody>
                  {result.ruleStats.map((r) => (
                    <tr key={r.rule}>
                      <td>{r.rule}</td>
                      <td>{r.firedOnFraud}</td>
                      <td>{r.firedOnLegitimate}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </section>
      </div>
    </div>
  );
}

function Metric({ label, value }) {
  return (
    <div className="metric">
      <div className="metric-label">{label}</div>
      <div className="metric-value">{value}</div>
    </div>
  );
}