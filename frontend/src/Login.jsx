import { useState } from "react";
import { Link } from "react-router-dom";
import { login, register } from "./api";
import Logo from "./Logo";
import ChartLoader from "./ChartLoader";

const DEMO = { username: "demo", password: "Demopass123!" };

const RULES = [
  { label: "At least 8 characters", test: (p) => p.length >= 8 },
  { label: "One uppercase letter", test: (p) => /[A-Z]/.test(p) },
  { label: "One lowercase letter", test: (p) => /[a-z]/.test(p) },
  { label: "One number", test: (p) => /[0-9]/.test(p) },
  { label: "One symbol", test: (p) => /[^A-Za-z0-9]/.test(p) },
];

export default function Login({ onAuthenticated }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [isRegistering, setIsRegistering] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [busyLabel, setBusyLabel] = useState("");

  const passwordOk = RULES.every((r) => r.test(password));
  const matches = password.length > 0 && password === confirm;

  function store(result) {
    localStorage.setItem("token", result.token);
    localStorage.setItem("username", result.username);
    onAuthenticated(result.username);
  }

  async function submit() {
    if (!username.trim()) {
      setError("Enter a username");
      return;
    }
    if (!password) {
      setError("Enter a password");
      return;
    }
    if (isRegistering && !passwordOk) {
      setError("Password does not meet the requirements");
      return;
    }
    if (isRegistering && !matches) {
      setError("Passwords do not match");
      return;
    }

    setBusy(true);
    setBusyLabel(isRegistering ? "Creating your account…" : "Signing you in…");
    setError("");
    try {
      const result = isRegistering
        ? await register(username, password)
        : await login(username, password);
      store(result);
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  }

  async function tryDemo() {
    setBusy(true);
    setBusyLabel("Opening the demo account…");
    setError("");
    try {
      store(await login(DEMO.username, DEMO.password));
    } catch {
      try {
        store(await register(DEMO.username, DEMO.password));
      } catch (e) {
        setError(e.message);
        setBusy(false);
      }
    }
  }

  function toggleMode() {
    setIsRegistering(!isRegistering);
    setError("");
    setPassword("");
    setConfirm("");
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <Link to="/" className="login-brand">
          <Logo size={30} />
          <span>Anomaly Engine</span>
        </Link>

        <h1>{isRegistering ? "Create an account" : "Sign in"}</h1>
        <p className="subtitle">
          {isRegistering
            ? "Your runs, alerts and review decisions are saved to your account."
            : "Pick up where you left off — your runs and reviews are waiting."}
        </p>

        {busy ? (
          <div className="login-busy">
            <ChartLoader label={busyLabel} />
          </div>
        ) : (
          <>
            <label htmlFor="username">Username</label>
            <input
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && submit()}
              autoFocus
            />

            <label htmlFor="password">Password</label>
            {isRegistering && (
              <p className="hint">
                8+ characters, with an uppercase letter, a lowercase letter, a number,
                and a symbol.
              </p>
            )}
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && submit()}
            />

            {isRegistering && (
              <>
                <ul className="rules">
                  {RULES.map((r) => (
                    <li key={r.label} className={r.test(password) ? "met" : ""}>
                      <span className="tick">{r.test(password) ? "✓" : "○"}</span>
                      {r.label}
                    </li>
                  ))}
                </ul>

                <label htmlFor="confirm">Confirm password</label>
                <input
                  id="confirm"
                  type="password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && submit()}
                />
                {confirm.length > 0 && !matches && (
                  <p className="mismatch">Passwords do not match</p>
                )}
              </>
            )}

            {error && <div className="error">{error}</div>}

            <button onClick={submit}>
              {isRegistering ? "Create account" : "Sign in"}
            </button>

            <button className="link" onClick={toggleMode}>
              {isRegistering
                ? "Already have an account? Sign in"
                : "Need an account? Register"}
            </button>

            <div className="divider"><span>or</span></div>

            <button className="secondary" onClick={tryDemo}>
              Try the demo
            </button>
            <p className="help demo-note">
              A shared account with sample data — no signup needed.
            </p>
          </>
        )}
      </div>
    </div>
  );
}