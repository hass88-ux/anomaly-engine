import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { uploadFile, startAnalysis, fetchJob } from "../api";

const FIELDS = [
  { key: "accountId", label: "Account", required: true,
    help: "The customer, card or account identifier" },
  { key: "occurredAt", label: "Timestamp", required: true,
    help: "When the transaction happened" },
  { key: "amount", label: "Amount", required: true,
    help: "Transaction value" },
  { key: "transactionId", label: "Transaction ID", required: false,
    help: "Optional. Row numbers are used if absent" },
  { key: "latitude", label: "Latitude", required: false,
    help: "Optional. Without it the impossible-travel rule is skipped" },
  { key: "longitude", label: "Longitude", required: false,
    help: "Optional. Needed alongside latitude" },
  { key: "isFraud", label: "Fraud label", required: false,
    help: "Optional. Without it precision and recall cannot be measured" },
];

const DEFAULT_CONFIG = {
  velocityMinCount: 3,
  velocityWindowMinutes: 3,
  velocitySpendMultiplier: 6.0,
  amountMultiplier: 4.0,
  amountMinHistory: 5,
  geoMaxSpeedKmh: 900.0,
};

function bytes(n) {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(0)} KB`;
  return `${(n / 1024 / 1024).toFixed(1)} MB`;
}

export default function Upload({ onJobComplete }) {
  const navigate = useNavigate();
  const fileInput = useRef(null);

  const [stage, setStage] = useState("pick");
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState("");

  const [preview, setPreview] = useState(null);
  const [mapping, setMapping] = useState(null);
  const [config, setConfig] = useState(DEFAULT_CONFIG);

  const [job, setJob] = useState(null);

  async function handleFile(file) {
    if (!file) return;

    setError("");
    setStage("uploading");

    try {
      const result = await uploadFile(file);
      setPreview({ ...result, originalName: file.name });
      setMapping(result.detected);
      setStage("mapping");
    } catch (e) {
      setError(e.message);
      setStage("pick");
    }
  }

  function onDrop(e) {
    e.preventDefault();
    setDragging(false);
    handleFile(e.dataTransfer.files?.[0]);
  }

  const canAnalyse =
    mapping && mapping.accountId && mapping.occurredAt && mapping.amount;

  async function analyse() {
    setError("");
    setStage("running");

    try {
      const { jobId } = await startAnalysis(preview.uploadId, {
        mapping,
        filename: preview.originalName,
        ...config,
      });

      const poll = setInterval(async () => {
        try {
          const current = await fetchJob(jobId);
          setJob(current);

          if (current.status === "COMPLETED" || current.status === "FAILED") {
            clearInterval(poll);
            setStage("done");
            if (current.status === "COMPLETED" && onJobComplete) {
              onJobComplete(jobId);
            }
          }
        } catch (e) {
          clearInterval(poll);
          setError(e.message);
          setStage("done");
        }
      }, 600);
    } catch (e) {
      setError(e.message);
      setStage("mapping");
    }
  }

  function reset() {
    setStage("pick");
    setPreview(null);
    setMapping(null);
    setJob(null);
    setError("");
    setConfig(DEFAULT_CONFIG);
  }

  return (
    <div className="page">
      <div className="page-head">
        <h1>Analyse your own data</h1>
        <p className="page-sub">
          Upload a CSV of transactions. Columns are detected automatically and shown for
          confirmation before anything is analysed. Files are deleted once analysis
          finishes.
        </p>
      </div>

      {error && <div className="error">{error}</div>}

      {stage === "pick" && (
        <div className="card">
          <div
            className={dragging ? "dropzone over" : "dropzone"}
            onDragOver={(e) => {
              e.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={onDrop}
            onClick={() => fileInput.current?.click()}
          >
            <h3>Drop a CSV here</h3>
            <p className="help">or click to choose a file — up to 25 MB</p>
            <input
              ref={fileInput}
              type="file"
              accept=".csv,.txt"
              style={{ display: "none" }}
              onChange={(e) => handleFile(e.target.files?.[0])}
            />
          </div>

          <div className="requirements">
            <h3>What the file needs</h3>
            <p className="help">
              At minimum: an account identifier, a timestamp, and an amount. Latitude and
              longitude enable the impossible-travel rule. A fraud label, if you have one,
              lets the run be scored for precision and recall.
            </p>
          </div>
        </div>
      )}

      {stage === "uploading" && (
        <div className="card">
          <div className="empty">
            <h3>Reading the file…</h3>
            <p>Only the header and first few rows are read at this stage.</p>
          </div>
        </div>
      )}

      {stage === "mapping" && preview && (
        <>
          <div className="card">
            <div className="card-head">
              <div>
                <h2>{preview.originalName}</h2>
                <p className="help">
                  {bytes(preview.sizeBytes)} · {preview.headers.length} columns
                </p>
              </div>
              <button className="review-btn" onClick={reset}>
                Choose a different file
              </button>
            </div>

            {preview.warnings?.length > 0 && (
              <ul className="warnings">
                {preview.warnings.map((w) => (
                  <li key={w}>{w}</li>
                ))}
              </ul>
            )}
          </div>

          <div className="card">
            <h2>Confirm the columns</h2>
            <p className="help" style={{ marginBottom: 18 }}>
              These were detected from your header row. Correct anything that looks wrong —
              analysing against the wrong column produces confident, wrong results.
            </p>

            <div className="mapping-grid">
              {FIELDS.map((field) => (
                <div className="mapping-row" key={field.key}>
                  <div className="mapping-label">
                    <strong>
                      {field.label}
                      {field.required && <span className="req">required</span>}
                    </strong>
                    <span className="help">{field.help}</span>
                  </div>
                  <select
                    value={mapping[field.key] || ""}
                    onChange={(e) =>
                      setMapping({ ...mapping, [field.key]: e.target.value || null })
                    }
                  >
                    <option value="">— not in this file —</option>
                    {preview.headers.map((h) => (
                      <option key={h} value={h}>{h}</option>
                    ))}
                  </select>
                </div>
              ))}
            </div>
          </div>

          <div className="card">
            <h2>Sample rows</h2>
            <p className="help" style={{ marginBottom: 14 }}>
              The first {preview.sampleRows.length} rows, so you can check the mapping
              against real values.
            </p>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    {preview.headers.map((h) => (
                      <th key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {preview.sampleRows.map((row, i) => (
                    <tr key={i}>
                      {preview.headers.map((h) => (
                        <td key={h} className="muted">{row[h]}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="card">
            <h2>Rule settings</h2>
            <p className="help" style={{ marginBottom: 18 }}>
              The shipped configuration, calibrated on generated data. Adjust if your data
              behaves differently.
            </p>

            <div className="config-grid">
              <label>
                Velocity min count
                <input
                  type="number" min="2" max="20"
                  value={config.velocityMinCount}
                  onChange={(e) =>
                    setConfig({ ...config, velocityMinCount: Number(e.target.value) })}
                />
              </label>
              <label>
                Velocity window (min)
                <input
                  type="number" min="1" max="120"
                  value={config.velocityWindowMinutes}
                  onChange={(e) =>
                    setConfig({ ...config, velocityWindowMinutes: Number(e.target.value) })}
                />
              </label>
              <label>
                Velocity spend multiplier
                <input
                  type="number" min="1" max="20" step="0.5"
                  value={config.velocitySpendMultiplier}
                  onChange={(e) =>
                    setConfig({ ...config, velocitySpendMultiplier: Number(e.target.value) })}
                />
              </label>
              <label>
                Amount multiplier
                <input
                  type="number" min="1" max="20" step="0.5"
                  value={config.amountMultiplier}
                  onChange={(e) =>
                    setConfig({ ...config, amountMultiplier: Number(e.target.value) })}
                />
              </label>
              <label>
                Amount min history
                <input
                  type="number" min="1" max="50"
                  value={config.amountMinHistory}
                  onChange={(e) =>
                    setConfig({ ...config, amountMinHistory: Number(e.target.value) })}
                />
              </label>
              <label>
                Max speed (km/h)
                <input
                  type="number" min="100" max="5000" step="50"
                  value={config.geoMaxSpeedKmh}
                  onChange={(e) =>
                    setConfig({ ...config, geoMaxSpeedKmh: Number(e.target.value) })}
                />
              </label>
            </div>

            <div className="actions">
              <button disabled={!canAnalyse} onClick={analyse}>
                Analyse {preview.originalName}
              </button>
              {!canAnalyse && (
                <span className="help">
                  Account, timestamp and amount must all be mapped
                </span>
              )}
            </div>
          </div>
        </>
      )}

      {(stage === "running" || stage === "done") && (
        <div className="card">
          <h2>
            {job?.status === "COMPLETED"
              ? "Analysis complete"
              : job?.status === "FAILED"
              ? "Analysis failed"
              : "Analysing…"}
          </h2>

          <div className="progress-track">
            <div
              className={
                job?.status === "FAILED" ? "progress-fill failed" : "progress-fill"
              }
              style={{ width: `${job?.percentComplete || 0}%` }}
            />
          </div>

          <p className="help">
            {job
              ? `${job.percentComplete}% · ${job.rowsRead.toLocaleString()} rows read`
              : "Starting…"}
          </p>

          {job?.status === "FAILED" && (
            <div className="error" style={{ marginTop: 16 }}>
              {job.failureReason}
            </div>
          )}

          {job?.status === "COMPLETED" && (
            <>
              <div className="metrics" style={{ marginTop: 24 }}>
                <div className="metric">
                  <div className="metric-label">Rows analysed</div>
                  <div className="metric-value">
                    {job.rowsAccepted.toLocaleString()}
                  </div>
                  {job.rowsRejected > 0 && (
                    <div className="metric-note">{job.rowsRejected} rejected</div>
                  )}
                </div>
                <div className="metric">
                  <div className="metric-label">Flagged accounts</div>
                  <div className="metric-value">{job.flaggedAccounts}</div>
                  <div className="metric-note">
                    {job.flaggedTransactions} transactions
                  </div>
                </div>
                <div className="metric">
                  <div className="metric-label">Precision</div>
                  <div className="metric-value">
                    {job.precision == null ? "—" : job.precision.toFixed(3)}
                  </div>
                  <div className="metric-note">
                    {job.precision == null ? "no fraud label in file" : "of alerts, correct"}
                  </div>
                </div>
                <div className="metric">
                  <div className="metric-label">Recall</div>
                  <div className="metric-value">
                    {job.recall == null ? "—" : job.recall.toFixed(3)}
                  </div>
                  <div className="metric-note">
                    {job.recall == null ? "cannot be measured" : "of fraud, caught"}
                  </div>
                </div>
              </div>

              {job.errors?.length > 0 && (
                <div className="parse-errors">
                  <h3>Rows that could not be read</h3>
                  <p className="help" style={{ marginBottom: 10 }}>
                    These were skipped. The rest of the file was analysed normally.
                  </p>
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr><th>Line</th><th>Problem</th></tr>
                      </thead>
                      <tbody>
                        {job.errors.map((e, i) => (
                          <tr key={i}>
                            <td className="muted">{e.line}</td>
                            <td className="muted">{e.message}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  {job.errorsTruncated && (
                    <p className="help">
                      Showing the first {job.errors.length} of {job.rowsRejected}.
                    </p>
                  )}
                </div>
              )}

              <div className="actions">
                <button onClick={() => navigate("/alerts")}>
                  View {job.flaggedAccounts} flagged accounts
                </button>
                <button className="review-btn" onClick={reset}>
                  Analyse another file
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}