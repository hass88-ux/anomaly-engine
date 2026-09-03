const BASE = import.meta.env.VITE_API_BASE ?? "/api";

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

export function fetchReviews() {
  return request("/reviews");
}

export function setReview(accountId, status, note = null) {
  return request(`/reviews/${encodeURIComponent(accountId)}`, {
    method: "PUT",
    body: JSON.stringify({ status, note }),
  });
}

export async function uploadFile(file) {
  const form = new FormData();
  form.append("file", file);

  const response = await fetch(`${BASE}/uploads`, {
    method: "POST",
    headers: authHeaders(),
    body: form,
  });

  if (response.status === 401 || response.status === 403) {
    sessionExpired();
    throw new Error("Your session expired. Please sign in again.");
  }

  const text = await response.text();
  const body = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(body?.error || `Upload failed (${response.status})`);
  }

  return body;
}

export function startAnalysis(uploadId, request_) {
  return request(`/uploads/${encodeURIComponent(uploadId)}/analyze`, {
    method: "POST",
    body: JSON.stringify(request_),
  });
}

export function fetchJob(id) {
  return request(`/jobs/${id}`);
}

export function fetchJobs() {
  return request("/jobs");
}

export function fetchJobAlerts(id) {
  return request(`/jobs/${id}/alerts`);
}