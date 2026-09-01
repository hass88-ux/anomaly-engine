import { useState } from "react";
import Login from "./Login";
import Sandbox from "./Sandbox";
import "./App.css";

export default function App() {
  const [username, setUsername] = useState(
    () => localStorage.getItem("username") || null
  );

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    setUsername(null);
  }

  if (!username) {
    return <Login onAuthenticated={setUsername} />;
  }

  return <Sandbox username={username} onLogout={logout} />;
}