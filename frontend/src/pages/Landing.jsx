import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Logo from "../Logo";
import "../landing.css";

const FEED = [
  { id: "TXN0042117", acct: "CUST-0314", amt: "$48.20", city: "Mississauga, ON", flag: null },
  { id: "TXN0042118", acct: "CUST-0088", amt: "$31.05", city: "Laval, QC", flag: null },
  { id: "TXN0042119", acct: "CUST-0314", amt: "$2.10", city: "Mississauga, ON", flag: null },
  { id: "TXN0042120", acct: "CUST-0314", amt: "$1.85", city: "Mississauga, ON", flag: null },
  { id: "TXN0042121", acct: "CUST-0314", amt: "$3.40", city: "Mississauga, ON",
    flag: "AmountOutlier", note: "4 transactions far below this account's $62 average" },
  { id: "TXN0042122", acct: "CUST-0561", amt: "$127.80", city: "Burnaby, BC", flag: null },
  { id: "TXN0042123", acct: "CUST-0561", amt: "$96.40", city: "Halifax, NS",
    flag: "GeoImpossibility", note: "4,400 km in 38 minutes — 6,900 km/h implied" },
];

export default function Landing() {
  const navigate = useNavigate();
  const [visible, setVisible] = useState(0);

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) {
      setVisible(FEED.length);
      return;
    }

    const timer = setInterval(() => {
      setVisible((n) => {
        if (n >= FEED.length) {
          clearInterval(timer);
          return n;
        }
        return n + 1;
      });
    }, 420);

    return () => clearInterval(timer);
  }, []);

  return (
    <div className="landing">
      <header className="landing-bar">
        <div className="brand">
          <Logo size={28} />
          <span>Anomaly Engine</span>
        </div>
        <nav>
          <Link to="/guide">How it works</Link>
          <a href="https://github.com/hass88-ux/anomaly-engine">Source</a>
          <button className="btn-primary" onClick={() => navigate("/login")}>
            Sign in
          </button>
        </nav>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <h1>
            Upload a CSV of transactions.<br />
            Get back the accounts worth investigating.
          </h1>
          <p className="lede">
            A rule-based fraud detection engine with a measurement harness attached. Every
            alert carries the transactions that triggered it, so you can see why it fired
            rather than trusting a score.
          </p>

          <div className="hero-actions">
            <button className="btn-primary lg" onClick={() => navigate("/login")}>
              Try it with your own data
            </button>
            <Link className="btn-ghost lg" to="/guide">
              How it works
            </Link>
          </div>

          <dl className="hero-stats">
            <div>
              <dt>Precision on unseen data</dt>
              <dd>0.978</dd>
            </div>
            <div>
              <dt>Replay throughput</dt>
              <dd>148k <span>rows / 600ms</span></dd>
            </div>
            <div>
              <dt>Rules, independently tunable</dt>
              <dd>3</dd>
            </div>
          </dl>
        </div>

        <div className="hero-feed" aria-hidden="true">
          <div className="feed-head">
            <span className="dot" />
            live replay
          </div>

          <div className="feed-rows">
            {FEED.slice(0, visible).map((row) => (
              <div className={row.flag ? "feed-row flagged" : "feed-row"} key={row.id}>
                <div className="feed-main">
                  <span className="mono id">{row.id}</span>
                  <span className="mono acct">{row.acct}</span>
                  <span className="mono amt">{row.amt}</span>
                  <span className="city">{row.city}</span>
                </div>
                {row.flag && (
                  <div className="feed-flag">
                    <span className="rule">{row.flag}</span>
                    <span className="why">{row.note}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="pillars">
        <article>
          <h2>Detection you can audit</h2>
          <p>
            Three rules — spend velocity, amount deviation, and implied travel speed.
            Each alert names which fired and shows the transactions behind it. No model,
            no score you have to take on faith.
          </p>
        </article>
        <article>
          <h2>Built to be measured</h2>
          <p>
            A seeded generator plants four known fraud patterns, so precision and recall
            are computed rather than estimated. Change a threshold, rerun, compare.
          </p>
        </article>
        <article>
          <h2>Handles real files</h2>
          <p>
            Columns are detected and confirmed. Currency symbols, eleven date formats and
            European decimals are coerced. Malformed rows are reported by line number
            instead of failing the upload.
          </p>
        </article>
      </section>

      <footer className="landing-foot">
        <span>Built by Muhammad Hassan Amir</span>
        <a href="https://github.com/hass88-ux/anomaly-engine">github.com/hass88-ux</a>
      </footer>
    </div>
  );
}