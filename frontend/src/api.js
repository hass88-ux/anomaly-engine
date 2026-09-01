const BASE = "http://localhost:8080/api";

function authHeaders() {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function sessionExpired() {
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  window.location.reload();
}

async function request(path, options = {}) {
  const isAuthCall = path.startsWith("/auth/");

  const response = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
      ...options.headers,
    },
  });

  if (!isAuthCall && (response.status === 401 || response.status === 403)) {
    sessionExpired();
    throw new Error("Your session expired. Please sign in again.");
  }

  const text = await response.text();
  const body = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(body?.error || `Request failed (${response.status})`);
  }

  return body;
}

export function login(username, password) {
  return request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export function register(username, password) {
  return request("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export function runReplay(params) {
  return request("/replay", {
    method: "POST",
    body: JSON.stringify(params),
  });
}

export function fetchHistory() {
  return request("/replay/history");
}