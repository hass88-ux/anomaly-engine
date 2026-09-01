import { useState } from "react";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import { runReplay } from "../api";

const DEFAULTS = {
  accounts: 400, days: 30, seed: 42,
  velocityMinCount: 3, velocityWindowMinutes: 3, velocitySpendMultiplier: 6.0,
  amountMultiplier: 4.0, amountMinHistory: 5, geoMaxSpeedKmh: 900.0,
};

const SWEEPS = {
  velocitySpendMultiplier: {
    label: "Spend velocity multiplier",
    values: [3, 4, 5, 6, 7, 8, 9, 10],
    blurb: "How much more than usual an account must spend in a short window before it is flagged.",
  },
  amountMultiplier: {
    label: "Amount outlier multiplier",
    values: [2, 3, 4, 5, 6, 7, 8],
    blurb: "How far a single transaction must deviate from the account's normal size.",
  },
  geoMaxSpeedKmh: {
    label: "Geo max speed (km/h)",
    values: [200, 400, 600, 800, 1000, 1400, 1800],
    blurb: "The implied travel speed above which two transactions cannot be the same person.",
  },
};

export default function Analysis({ params }) {
  const base = params || DEFAULTS;
  const [key, setKey] = useState("velocitySpendMultiplier");
  const [points, setPoints] = useState([]);
  const [busy, setBusy] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState("");

  const sweep = SWEEPS[key];

  async function run() {
    setBusy(true);
    setError("");
    setPoints([]);
    setProgress(0);

    const collected = [];
    try {
      for (const value of sweep.values) {
        const r = await runReplay({ ...base, [key]: value });
        collected.push({
          setting: value,
          precision: Number(r.precision.toFixed(3)),
          recall: Number(r.recall.toFixed(3)),
          caught: r.truePositives,
          falseAlarms: r.falsePositives,
        });
        setPoints([...collected]);
        setProgress(collected.length);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <h1>Analysis</h1>
        <p className="page-sub">
          Every detection rule trades accuracy against coverage. Tighten it and fewer
          innocent customers get flagged, but more fraud slips through. This sweeps one
          setting across its range so you can see where that trade turns bad.
        </p>
      </div>

      <section className="card">
        <div className="card-head">
          <div>
            <h2>Sweep a setting</h2>
            <p className="help">{sweep.blurb}</p>
          </div>
          <div className="sweep-controls">
            <select value={key} onChange={(e) => { setKey(e.target.value); setPoints([]); }}>
              {Object.entries(SWEEPS).map(([k, s]) => (
                <option key={k} value={k}>{s.label}</option>
              ))}
            </select>
            <button onClick={run} disabled={busy}>
              {busy ? `Running ${progress}/${sweep.values.length}…` : "Run sweep"}
            </button>
          </div>
        </div>

        {error && <div className="error">{error}</div>}

        {points.length === 0 && !busy && (
          <div className="empty">
            <p>
              Runs {sweep.values.length} tests against the same dataset, changing only
              this one setting. Takes a few seconds.
            </p>
          </div>
        )}

        {points.length > 0 && (
          <>
            <div style={{ width: "100%", height: 340, marginTop: 20 }}>
              <ResponsiveContainer>
                <LineChart data={points} margin={{ top: 8, right: 16, bottom: 32, left: 0 }}>
                  <CartesianGrid stroke="#e3e8ee" vertical={false} />
                  <XAxis dataKey="setting" tick={{ fontSize: 12, fill: "#697386" }}
                    label={{ value: sweep.label, position: "insideBottom", offset: -18,
                             fontSize: 12, fill: "#697386" }} />
                  <YAxis domain={[0, 1]} tick={{ fontSize: 12, fill: "#697386" }} />
                  <Tooltip />
                  <Legend verticalAlign="top" align="right" height={28}
                          wrapperStyle={{ fontSize: 13 }} />
                  <Line type="monotone" dataKey="precision" name="Precision"
                        stroke="#635bff" strokeWidth={2} dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="recall" name="Recall"
                        stroke="#0a7d55" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <p className="plain">
              Precision is the share of flags that were real fraud. Recall is the share
              of real fraud that was caught. Where the precision line flattens, further
              tightening buys nothing and only costs recall.
            </p>

            <h2>Every point on the curve</h2>
            <table>
              <thead>
                <tr>
                  <th>{sweep.label}</th>
                  <th>Precision</th><th>Recall</th>
                  <th>Caught</th><th>False alarms</th>
                </tr>
              </thead>
              <tbody>
                {points.map((p) => (
                  <tr key={p.setting}>
                    <td>{p.setting}</td>
                    <td>{p.precision.toFixed(3)}</td>
                    <td>{p.recall.toFixed(3)}</td>
                    <td>{p.caught}</td>
                    <td>{p.falseAlarms}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </section>
    </div>
  );
}