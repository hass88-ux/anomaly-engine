export default function ChartLoader({ percent = null, label = null }) {
  const bars = 12;
  const indeterminate = percent === null;

  return (
    <div className="chart-loader" role="status" aria-live="polite">
      <div className={indeterminate ? "chart-bars pulsing" : "chart-bars"}>
        {Array.from({ length: bars }).map((_, i) => {
          const threshold = ((i + 1) / bars) * 100;
          const filled = indeterminate || percent >= threshold;
          const height = 18 + (i / (bars - 1)) * 46;

          return (
            <span
              key={i}
              className={filled ? "chart-bar on" : "chart-bar"}
              style={{
                height: `${height}%`,
                animationDelay: indeterminate ? `${i * 0.07}s` : undefined,
              }}
            />
          );
        })}
      </div>
      {label && <p className="chart-loader-label">{label}</p>}
    </div>
  );
}