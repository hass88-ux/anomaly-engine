import { NavLink, Outlet } from "react-router-dom";
const NAV = [

  { to: "/", label: "Overview", end: true },
  { to: "/run", label: "Run a test" },
  { to: "/upload", label: "Upload data" },
  { to: "/alerts", label: "Alerts" },
  { to: "/analysis", label: "Analysis" },
  { to: "/history", label: "History" },
  { to: "/manual", label: "Manual" },
];

export default function Layout({ username, onLogout }) {
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <span className="mark">AE</span>
          <span>Anomaly Engine</span>
        </div>
        <nav className="topbar-nav">
          <NavLink to="/manual">User manual</NavLink>
          <span className="who">{username}</span>
          <button className="link" onClick={onLogout}>Sign out</button>
        </nav>
      </header>

      <div className="body">
        <aside className="sidebar">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? "nav-item active" : "nav-item")}
            >
              {item.label}
            </NavLink>
          ))}
        </aside>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}