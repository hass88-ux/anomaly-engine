import { useState } from "react";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import { runReplay } from "./api";

const SWEEP = [3, 4, 5, 6, 7, 8, 9, 10];

export default function Curve({ params }) {
  const [points, setPoints] = useState([]);
  const [busy, setBusy] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState("");

  async function sweep() {
    setBusy(true);
    setError("");
    setPoints([]);
    setProgress(0);

    const collected = [];
    try {
      for (const multiplier of SWEEP) {
        const result = await runReplay({
          ...params,
          velocitySpendMultiplier: multiplier,
        });
        collected.push({
          multiplier,
          precision: Number(result.precision.toFixed(3)),
          recall: Number(result.recall.toFixed(3)),
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
    <section className="results" style={{ marginTop: 24 }}>
      <div className="curve-head">
        <div>
          <h2>Precision / recall trade-off</h2>
          <p className="help">
            Runs the spend velocity rule at eight thresholds against the same dataset.
            Tightening the rule raises precision and costs recall — the useful question
            is where the exchange rate turns bad.
          </p>
        </div>
        <button onClick={sweep} disabled={busy}>
          {busy ? `Running ${progress}/${SWEEP.length}…` : "Run sweep"}
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {points.length > 0 && (
        <>
          <div style={{ width: "100%", height: 320, marginTop: 20 }}>
            <ResponsiveContainer>
              <LineChart data={points} margin={{ top: 8, right: 16, bottom: 32, left: 0 }}>
                <CartesianGrid stroke="#e3e8ee" vertical={false} />
                <XAxis
                  dataKey="multiplier"
                  tick={{ fontSize: 12, fill: "#697386" }}
                  label={{
                    value: "Spend velocity multiplier",
                    position: "insideBottom",
                    offset: -18,
                    fontSize: 12,
                    fill: "#697386",
                  }}
                />
                <YAxis domain={[0, 1]} tick={{ fontSize: 12, fill: "#697386" }} />
                <Tooltip />
                <Legend
                  verticalAlign="top"
                  align="right"
                  height={28}
                  wrapperStyle={{ fontSize: 13 }}
                />
                <Line
                  type="monotone"
                  dataKey="precision"
                  name="Precision"
                  stroke="#635bff"
                  strokeWidth={2}
                  dot={{ r: 3 }}
                />
                <Line
                  type="monotone"
                  dataKey="recall"
                  name="Recall"
                  stroke="#0a7d55"
                  strokeWidth={2}
                  dot={{ r: 3 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {points.length > 1 && (
            <p className="help" style={{ marginTop: 12 }}>
              Precision rises sharply below 5 and then flattens; recall falls steadily
              throughout. The best operating point sits at the knee — past it,
              tightening the rule costs coverage without buying accuracy.
            </p>
          )}
        </>
      )}
    </section>
  );
}