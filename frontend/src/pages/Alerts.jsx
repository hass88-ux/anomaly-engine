import { useState } from "react";
import { Link } from "react-router-dom";

const money = new Intl.NumberFormat("en-CA", {
  style: "currency",
  currency: "CAD",
  maximumFractionDigits: 0,
});

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

export default function Alerts({ lastResult }) {
  const [expanded, setExpanded] = useState(() => new Set());
  const [filter, setFilter] = useState("ALL");

  if (!lastResult) {
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
            <h3>No test run yet</h3>
            <p>Alerts appear here after a test.</p>
            <Link className="btn" to="/run">Run a test</Link>
          </div>
        </div>
      </div>
    );
  }

  const all = lastResult.accountAlerts || [];
  const total = lastResult.totalFlaggedAccounts || 0;
  const shown = filter === "ALL" ? all : all.filter((a) => a.confidence === filter);

  function toggle(accountId) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(accountId)) {
        next.delete(accountId);
      } else {
        next.add(accountId);
      }
      return next;
    });
  }

  const counts = {
    HIGH: all.filter((a) => a.confidence === "HIGH").length,
    MEDIUM: all.filter((a) => a.confidence === "MEDIUM").length,
    LOW: all.filter((a) => a.confidence === "LOW").length,
  };

  return (
    <div className="page">
      <div className="page-head">
        <h1>Alerts</h1>
        <p className="page-sub">
          Accounts flagged by the rules, with the transactions that triggered each flag.
          Confidence is derived from how many transactions were flagged and how many
          distinct rules agreed.
        </p>
      </div>

      {all.length === 0 ? (
        <div className="card">
          <div className="empty">
            <h3>Nothing flagged</h3>
            <p>The last run produced no alerts. Loosen the thresholds and run again.</p>
            <Link className="btn" to="/run">Adjust settings</Link>
          </div>
        </div>
      ) : (
        <>
          <div className="card">
            <div className="card-head">
              <div>
                <h2>
                  Showing {all.length} of {total} flagged account
                  {total === 1 ? "" : "s"}
                </h2>
                <p className="help">
                  {total > all.length
                    ? `Capped at ${all.length} to keep the response small. Highest confidence first.`
                    : "Highest confidence first."}
                </p>
              </div>
            </div>

            <div className="filters">
              {["ALL", "HIGH", "MEDIUM", "LOW"].map((level) => (
                <button
                  key={level}
                  className={filter === level ? "chip active" : "chip"}
                  onClick={() => setFilter(level)}
                >
                  {level === "ALL" ? `All (${all.length})` : `${level} (${counts[level]})`}
                </button>
              ))}
            </div>
          </div>

          {shown.map((account) => {
            const isOpen = expanded.has(account.accountId);
            return (
              <div className="card alert-card" key={account.accountId}>
                <button className="alert-head" onClick={() => toggle(account.accountId)}>
                  <span className="alert-id">
                    <span className={`badge ${account.confidence.toLowerCase()}`}>
                      {account.confidence}
                    </span>
                    <strong>{account.accountId}</strong>
                  </span>

                  <span className="alert-facts">
                    <span>
                      {account.flaggedTransactions} flagged
                    </span>
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
                  {account.anyActuallyFraud && (
                    <span className="tag truth">contains real fraud</span>
                  )}
                </div>

                {isOpen && (
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>When</th>
                          <th>Amount</th>
                          <th>Location</th>
                          <th>Rules fired</th>
                          <th>Verdict</th>
                        </tr>
                      </thead>
                      <tbody>
                        {account.transactions.map((txn) => (
                          <tr key={txn.transactionId}>
                            <td className="muted">{when(txn.occurredAt)}</td>
                            <td>{money.format(txn.amount)}</td>
                            <td className="muted">
                              {txn.latitude.toFixed(2)}, {txn.longitude.toFixed(2)}
                            </td>
                            <td>
                              {txn.firedRules.map((rule) => (
                                <span className="tag" key={rule}>{rule}</span>
                              ))}
                            </td>
                            <td className={txn.actuallyFraud ? "hit" : "muted"}>
                              {txn.actuallyFraud ? "true positive" : "false positive"}
                            </td>
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