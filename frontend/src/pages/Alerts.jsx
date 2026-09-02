import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchReviews, setReview, fetchJobAlerts, fetchJob } from "../api";

const money = new Intl.NumberFormat("en-CA", {
  style: "currency",
  currency: "CAD",
  maximumFractionDigits: 0,
});

const STATUSES = ["NEW", "REVIEWED", "ESCALATED", "DISMISSED"];

function when(value) {
  const date = typeof value === "number" ? new Date(value * 1000) : new Date(value);
  return Number.isNaN(date.getTime())
    ? String(value)
    : date.toLocaleString("en-CA", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
}

export default function Alerts({ lastResult, jobId }) {
  const [expanded, setExpanded] = useState(() => new Set());
  const [confidence, setConfidence] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [reviews, setReviews] = useState({});
  const [saving, setSaving] = useState(() => new Set());
  const [editingNote, setEditingNote] = useState(null);
  const [noteDraft, setNoteDraft] = useState("");
  const [error, setError] = useState("");

  const [jobData, setJobData] = useState(null);
  const [jobMeta, setJobMeta] = useState(null);
  const [loadingJob, setLoadingJob] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetchReviews()
      .then((rows) => {
        if (cancelled) return;
        const map = {};
        (rows || []).forEach((r) => {
          map[r.accountId] = { status: r.status, note: r.note };
        });
        setReviews(map);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!jobId) {
      setJobData(null);
      setJobMeta(null);
      return;
    }

    let cancelled = false;
    setLoadingJob(true);

    Promise.all([fetchJobAlerts(jobId), fetchJob(jobId)])
      .then(([alerts, meta]) => {
        if (cancelled) return;
        setJobData(alerts);
        setJobMeta(meta);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      })
      .finally(() => {
        if (!cancelled) setLoadingJob(false);
      });

    return () => {
      cancelled = true;
    };
  }, [jobId]);

  const source = jobId ? jobData : lastResult;
  const hasGroundTruth = jobId ? jobMeta?.hasGroundTruth !== false : true;
  const sourceLabel = jobId ? jobMeta?.filename : "the last generated run";

  if (loadingJob) {
    return (
      <div className="page">
        <div className="page-head"><h1>Alerts</h1></div>
        <div className="card">
          <div className="empty"><h3>Loading alerts…</h3></div>
        </div>
      </div>
    );
  }

  if (!source) {
    return (
      <div className="page">
        <div className="page-head">
          <h1>Alerts</h1>
          <p className="page-sub">
            Accounts flagged by the rules, with the transactions that triggered each flag.
          </p>
        </div>
        <div className="card">
          <div className="empty">
            <h3>Nothing to show yet</h3>
            <p>Run a test on generated data, or upload your own transactions.</p>
            <div className="cta" style={{ justifyContent: "center" }}>
              <Link className="btn" to="/run">Run a test</Link>
              <Link className="btn-quiet" to="/upload">Upload data</Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const all = source.accountAlerts || [];
  const total = source.totalFlaggedAccounts || 0;

  function statusOf(accountId) {
    return reviews[accountId]?.status || "NEW";
  }

  function noteOf(accountId) {
    return reviews[accountId]?.note || "";
  }

  const shown = all.filter((a) => {
    const byConfidence = confidence === "ALL" || a.confidence === confidence;
    const byStatus = statusFilter === "ALL" || statusOf(a.accountId) === statusFilter;
    return byConfidence && byStatus;
  });

  function toggle(accountId) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(accountId)) next.delete(accountId);
      else next.add(accountId);
      return next;
    });
  }

  async function persist(accountId, status, note, previous) {
    setSaving((prev) => new Set(prev).add(accountId));
    try {
      await setReview(accountId, status, note);
    } catch (e) {
      setReviews((prev) => {
        const copy = { ...prev };
        if (previous) copy[accountId] = previous;
        else delete copy[accountId];
        return copy;
      });
      setError(`Could not save review for ${accountId}: ${e.message}`);
    } finally {
      setSaving((prev) => {
        const copy = new Set(prev);
        copy.delete(accountId);
        return copy;
      });
    }
  }

  async function mark(accountId, next) {
    const previous = reviews[accountId];
    const target = statusOf(accountId) === next ? "NEW" : next;
    const note = target === "NEW" ? null : previous?.note ?? null;

    setError("");
    setReviews((prev) => {
      const copy = { ...prev };
      if (target === "NEW") delete copy[accountId];
      else copy[accountId] = { status: target, note };
      return copy;
    });

    if (target === "NEW" && editingNote === accountId) {
      setEditingNote(null);
    }

    await persist(accountId, target, note, previous);
  }

  function startNote(accountId) {
    setEditingNote(accountId);
    setNoteDraft(noteOf(accountId));
  }

  async function saveNote(accountId) {
    const previous = reviews[accountId];
    const status = statusOf(accountId);
    const note = noteDraft.trim() || null;

    setError("");
    setEditingNote(null);
    setReviews((prev) => ({ ...prev, [accountId]: { status, note } }));

    await persist(accountId, status, note, previous);
  }

  const confidenceCounts = {
    HIGH: all.filter((a) => a.confidence === "HIGH").length,
    MEDIUM: all.filter((a) => a.confidence === "MEDIUM").length,
    LOW: all.filter((a) => a.confidence === "LOW").length,
  };

  const statusCounts = {};
  STATUSES.forEach((s) => {
    statusCounts[s] = all.filter((a) => statusOf(a.accountId) === s).length;
  });

  return (
    <div className="page">
      <div className="page-head">
        <h1>Alerts</h1>
        <p className="page-sub">
          Accounts flagged by the rules, with the transactions that triggered each flag.
          Confidence is derived from how many transactions were flagged and how many
          distinct rules agreed. Review decisions and notes are saved to your account.
        </p>
      </div>

      <div className="source-bar">
        <span className="tag">{jobId ? "uploaded file" : "generated data"}</span>
        <span className="help">Showing results from {sourceLabel}</span>
        {jobId && !hasGroundTruth && (
          <span className="help">
            · no fraud label in this file, so alerts cannot be scored
          </span>
        )}
      </div>

      {error && <div className="error">{error}</div>}

      {all.length === 0 ? (
        <div className="card">
          <div className="empty">
            <h3>Nothing flagged</h3>
            <p>This run produced no alerts. Loosen the thresholds and try again.</p>
            <Link className="btn" to={jobId ? "/upload" : "/run"}>Adjust settings</Link>
          </div>
        </div>
      ) : (
        <>
          <div className="card">
            <div className="card-head">
              <div>
                <h2>
                  Showing {shown.length} of {all.length} loaded
                  {total > all.length ? ` (${total} flagged in total)` : ""}
                </h2>
                <p className="help">
                  {total > all.length
                    ? `Capped at ${all.length} to keep the response small. Highest confidence first.`
                    : "Highest confidence first."}
                </p>
              </div>
            </div>

            <div className="filter-row">
              <span className="filter-label">Confidence</span>
              <div className="filters">
                {["ALL", "HIGH", "MEDIUM", "LOW"].map((level) => (
                  <button
                    key={level}
                    className={confidence === level ? "chip active" : "chip"}
                    onClick={() => setConfidence(level)}
                  >
                    {level === "ALL"
                      ? `All (${all.length})`
                      : `${level} (${confidenceCounts[level]})`}
                  </button>
                ))}
              </div>
            </div>

            <div className="filter-row">
              <span className="filter-label">Status</span>
              <div className="filters">
                <button
                  className={statusFilter === "ALL" ? "chip active" : "chip"}
                  onClick={() => setStatusFilter("ALL")}
                >
                  All ({all.length})
                </button>
                {STATUSES.map((s) => (
                  <button
                    key={s}
                    className={statusFilter === s ? "chip active" : "chip"}
                    onClick={() => setStatusFilter(s)}
                  >
                    {s} ({statusCounts[s]})
                  </button>
                ))}
              </div>
            </div>
          </div>

          {shown.length === 0 && (
            <div className="card">
              <div className="empty">
                <h3>Nothing matches</h3>
                <p>No accounts match this combination of filters.</p>
              </div>
            </div>
          )}

          {shown.map((account) => {
            const isOpen = expanded.has(account.accountId);
            const status = statusOf(account.accountId);
            const note = noteOf(account.accountId);
            const busy = saving.has(account.accountId);
            const isEditing = editingNote === account.accountId;

            return (
              <div
                className={
                  status === "DISMISSED" ? "card alert-card dimmed" : "card alert-card"
                }
                key={account.accountId}
              >
                <button className="alert-head" onClick={() => toggle(account.accountId)}>
                  <span className="alert-id">
                    <span className={`badge ${account.confidence.toLowerCase()}`}>
                      {account.confidence}
                    </span>
                    <strong>{account.accountId}</strong>
                    {status !== "NEW" && (
                      <span className={`status-pill ${status.toLowerCase()}`}>{status}</span>
                    )}
                    {note && <span className="note-dot">note</span>}
                  </span>

                  <span className="alert-facts">
                    <span>{account.flaggedTransactions} flagged</span>
                    <span>
                      {account.distinctRules} rule{account.distinctRules === 1 ? "" : "s"}
                    </span>
                    <span>{money.format(account.totalFlaggedAmount)}</span>
                    <span className="caret">{isOpen ? "▾" : "▸"}</span>
                  </span>
                </button>

                <div className="alert-rules">
                  {account.rulesTriggered.map((rule) => (
                    <span className="tag" key={rule}>{rule}</span>
                  ))}
                  {hasGroundTruth && account.anyActuallyFraud && (
                    <span className="tag truth">contains real fraud</span>
                  )}
                </div>

                <div className="review-bar">
                  {["REVIEWED", "ESCALATED", "DISMISSED"].map((s) => (
                    <button
                      key={s}
                      className={status === s ? "review-btn active" : "review-btn"}
                      disabled={busy}
                      onClick={() => mark(account.accountId, s)}
                    >
                      {s === "REVIEWED"
                        ? "Mark reviewed"
                        : s === "ESCALATED"
                        ? "Escalate"
                        : "Dismiss"}
                    </button>
                  ))}

                  <button
                    className="review-btn"
                    disabled={busy || status === "NEW"}
                    title={
                      status === "NEW"
                        ? "Set a status before adding a note"
                        : "Add or edit a note"
                    }
                    onClick={() => startNote(account.accountId)}
                  >
                    {note ? "Edit note" : "Add note"}
                  </button>

                  {busy && <span className="help">saving…</span>}
                </div>

                {status === "NEW" && (
                  <p className="help note-hint">
                    Notes attach to a decision — mark the account reviewed, escalated or
                    dismissed first.
                  </p>
                )}

                {note && !isEditing && <p className="note-text">{note}</p>}

                {isEditing && (
                  <div className="note-editor">
                    <textarea
                      value={noteDraft}
                      maxLength={500}
                      rows={3}
                      placeholder="Why did you make this call?"
                      onChange={(e) => setNoteDraft(e.target.value)}
                    />
                    <div className="note-actions">
                      <span className="help">{noteDraft.length}/500</span>
                      <button className="review-btn" onClick={() => setEditingNote(null)}>
                        Cancel
                      </button>
                      <button
                        className="review-btn active"
                        onClick={() => saveNote(account.accountId)}
                      >
                        Save note
                      </button>
                    </div>
                  </div>
                )}

                {isOpen && (
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>When</th>
                          <th>Amount</th>
                          <th>Location</th>
                          <th>Rules fired</th>
                          {hasGroundTruth && <th>Verdict</th>}
                        </tr>
                      </thead>
                      <tbody>
                        {account.transactions.map((txn) => (
                          <tr key={txn.transactionId}>
                            <td className="muted">{when(txn.occurredAt)}</td>
                            <td>{money.format(txn.amount)}</td>
                            <td>
                              {txn.city || "—"}
                              {txn.province && <span className="sub">{txn.province}</span>}
                            </td>
                            <td>
                              {txn.firedRules.map((rule) => (
                                <span className="tag" key={rule}>{rule}</span>
                              ))}
                            </td>
                            {hasGroundTruth && (
                              <td className={txn.actuallyFraud ? "hit" : "muted"}>
                                {txn.actuallyFraud ? "true positive" : "false positive"}
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            );
          })}
        </>
      )}
    </div>
  );
}