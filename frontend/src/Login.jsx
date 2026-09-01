import { useState } from "react";
import { login, register } from "./api";

export default function Login({ onAuthenticated }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isRegistering, setIsRegistering] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (!username.trim() || !password) {
      setError("Enter a username and password");
      return;
    }

    setBusy(true);
    setError("");

    try {
      const result = isRegistering
        ? await register(username, password)
        : await login(username, password);

      localStorage.setItem("token", result.token);
      localStorage.setItem("username", result.username);
      onAuthenticated(result.username);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <h1>Anomaly Engine</h1>
        <p className="subtitle">
          {isRegistering ? "Create an account" : "Sign in to continue"}
        </p>

        <label>Username</label>
        <input
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          autoFocus
        />

        <label>Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
        />

        {error && <div className="error">{error}</div>}

        <button onClick={submit} disabled={busy}>
          {busy ? "Working…" : isRegistering ? "Create account" : "Sign in"}
        </button>

        <button
          className="link"
          onClick={() => {
            setIsRegistering(!isRegistering);
            setError("");
          }}
        >
          {isRegistering
            ? "Already have an account? Sign in"
            : "Need an account? Register"}
        </button>
      </div>
    </div>
  );
}