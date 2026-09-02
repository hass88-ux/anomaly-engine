import { useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./Login";
import Layout from "./Layout";
import Overview from "./pages/Overview";
import RunTest from "./pages/RunTest";
import Upload from "./pages/Upload";
import Alerts from "./pages/Alerts";
import Analysis from "./pages/Analysis";
import History from "./pages/History";
import Manual from "./pages/Manual";
import "./App.css";

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
    return <Login onAuthenticated={setUsername} />;
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout username={username} onLogout={logout} />}>
          <Route index element={<Overview username={username} lastResult={lastResult} />} />
          <Route path="run" element={
            <RunTest onResult={handleResult} params={params} setParams={setParams} />
          } />
          <Route path="upload" element={<Upload onJobComplete={handleJobComplete} />} />
          <Route path="alerts" element={<Alerts lastResult={lastResult} jobId={jobId} />} />
          <Route path="analysis" element={<Analysis params={params} />} />
          <Route path="history" element={<History />} />
          <Route path="manual" element={<Manual />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}