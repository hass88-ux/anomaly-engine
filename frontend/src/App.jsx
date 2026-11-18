import { lazy, Suspense, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Landing from "./pages/Landing";
import Guide from "./pages/Guide";
import Login from "./Login";
import Layout from "./Layout";
import Overview from "./pages/Overview";
import Upload from "./pages/Upload";
import Alerts from "./pages/Alerts";
import History from "./pages/History";
import Manual from "./pages/Manual";
import "./App.css";

const RunTest = lazy(() => import("./pages/RunTest"));
const Analysis = lazy(() => import("./pages/Analysis"));

export default function App() {
  const [username, setUsername] = useState(
    () => localStorage.getItem("username") || null
  );
  const [lastResult, setLastResult] = useState(null);
  const [params, setParams] = useState(null);
  const [jobId, setJobId] = useState(null);

  function handleResult(result) {
    setLastResult(result);
    setJobId(null);
  }

  function handleJobComplete(id) {
    setJobId(id);
    setLastResult(null);
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    setUsername(null);
    setLastResult(null);
    setJobId(null);
  }

  if (!username) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/guide" element={<Guide />} />
          <Route path="/login" element={<Login onAuthenticated={setUsername} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/guide" element={<Guide />} />
        <Route element={<Layout username={username} onLogout={logout} />}>
          <Route index element={<Overview username={username} lastResult={lastResult} />} />
          <Route path="run" element={
            <Suspense fallback={<div className="page" role="status">Loading replay settings…</div>}>
              <RunTest onResult={handleResult} params={params} setParams={setParams} />
            </Suspense>
          } />
          <Route path="upload" element={<Upload onJobComplete={handleJobComplete} />} />
            <Route path="alerts" element={<Alerts key={jobId ?? "replay"} lastResult={lastResult} jobId={jobId} />} />
          <Route path="analysis" element={
            <Suspense fallback={<div className="page" role="status">Loading analysis…</div>}>
              <Analysis params={params} />
            </Suspense>
          } />
          <Route path="history" element={<History />} />
          <Route path="manual" element={<Manual />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
