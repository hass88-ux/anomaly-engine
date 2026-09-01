import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { runReplay } from "../api";

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

const DATASET_CONTROLS = [
  { key: "seed", label: "Dataset seed", min: 1, max: 200, step: 1,
    help: "Changes the dataset entirely. Findings that survive a seed change are real" },
  { key: "accounts", label: "Accounts", min: 50, max: 2000, step: 50,
    help: "Roughly 46 transactions per account" },
];

const RULE_CONTROLS = [
  { key: "velocitySpendMultiplier", label: "Spend velocity multiplier", min: 2, max: 12, step: 0.5,
    help: "Flags an account spending far more than usual in a short window" },
  { key: "velocityMinCount", label: "Spend velocity min count", min: 2, max: 10, step: 1,
    help: "Transactions needed in the window before the rule can fire" },
  { key: "velocityWindowMinutes", label: "Spend velocity window (min)", min: 1, max: 30, step: 1,
    help: "How far back the rule looks" },
  { key: "amountMultiplier", label: "Amount outlier multiplier", min: 2, max: 10, step: 0.5,
    help: "Flags single transactions far above or below the account's normal size" },
  { key: "amountMinHistory", label: "Amount outlier min history", min: 1, max: 20, step: 1,
    help: "Prior transactions needed before a baseline is trusted" },
  { key: "geoMaxSpeedKmh", label: "Geo max speed (km/h)", min: 100, max: 2000, step: 50,
    help: "Flags a card used in two places too far apart to travel between" },
];

export default function RunTest({ onResult, params, setParams }) {
  const values = params || DEFAULTS;
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  function update(key, value) {
    setParams({ ...values, [key]: Number(value) });
  }

  async function run() {
    setBusy(true);
    setError("");
    try {
      const r = await runReplay(values);
      setResult(r);
      onResult(r);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <h1>Run a test</h1>
        <p className="page-sub">
          Generate a transaction dataset with fraud planted in it, then run the
          detection rules against it. Because the fraud is known, accuracy can be
          measured exactly.
        </p>
      </div>

      <div className="two-col">
        <section className="card">
          <h2>Dataset</h2>
          {DATASET_CONTROLS.map((c) => (
            <Slider key={c.key} control={c} value={values[c.key]} onChange={update} />
          ))}

          <h2 style={{ marginTop: 28 }}>Rules</h2>
          {RULE_CONTROLS.map((c) => (
            <Slider key={c.key} control={c} value={values[c.key]} onChange={update} />
          ))}

          <div className="actions">
            <button onClick={run} disabled={busy}>
              {busy ? "Running…" : "Run test"}
            </button>
            <button className="link" onClick={() => setParams(DEFAULTS)}>
              Reset to defaults
            </button>
          </div>

          {error && <div className="error">{error}</div>}
        </section>

        <section className="card">
          {!result && !busy && (
            <div className="empty">
              <h3>No results yet</h3>
              <p>
                Press <strong>Run test</strong>. It takes under a second and scans
                roughly {values.accounts * 46} transactions.
              </p>
            </div>
          )}

          {busy && <div className="empty"><p>Running the engine…</p></div>}

          {result && !busy && (
            <>
              <p className="plain">
                Flagged <strong>{result.truePositives + result.falsePositives}</strong>{" "}
                transactions. <strong>{result.truePositives}</strong> were real fraud,{" "}
                <strong>{result.falsePositives}</strong> were false alarms, and{" "}
                <strong>{result.falseNegatives}</strong> frauds slipped through.
              </p>

              <div className="metrics">
                <Metric label="Precision" value={result.precision.toFixed(3)} />
                <Metric label="Recall" value={result.recall.toFixed(3)} />
                <Metric label="Caught" value={result.truePositives} />
                <Metric label="False alarms" value={result.falsePositives} />
                <Metric label="Missed" value={result.falseNegatives} />
                <Metric label="Time" value={`${result.replayTimeMs} ms`} />
              </div>

              <h2>Detection by attack type</h2>
              <table>
                <thead>
                  <tr><th>Pattern</th><th>Caught</th><th>Total</th><th>Rate</th></tr>
                </thead>
                <tbody>
                  {result.patternStats.map((p) => (
                    <tr key={p.pattern}>
                      <td>{p.pattern.replace(/_/g, " ").toLowerCase()}</td>
                      <td>{p.caught}</td>
                      <td>{p.total}</td>
                      <td>{(p.rate * 100).toFixed(1)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <h2>Which rule caught what</h2>
              <table>
                <thead>
                  <tr><th>Rule</th><th>Real fraud</th><th>False alarms</th></tr>
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

              <div className="cta">
                <button className="btn-quiet" onClick={() => navigate("/alerts")}>
                  View flagged accounts
                </button>
                <button className="btn-quiet" onClick={() => navigate("/analysis")}>
                  Compare thresholds
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}

function Slider({ control, value, onChange }) {
  return (
    <div className="control">
      <div className="control-head">
        <label>{control.label}</label>
        <span className="value">{value}</span>
      </div>
      <input type="range" min={control.min} max={control.max} step={control.step}
             value={value} onChange={(e) => onChange(control.key, e.target.value)} />
      <p className="help">{control.help}</p>
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