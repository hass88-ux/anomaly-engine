export default function Logo({ size = 28, boxed = true }) {
  const dots = [
    { cx: 6, cy: 18 },
    { cx: 11, cy: 16.5 },
    { cx: 16, cy: 17.5 },
    { cx: 21, cy: 16 },
  ];

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 28 28"
      fill="none"
      role="img"
      aria-label="Anomaly Engine"
    >
      {boxed && (
        <rect
          x="0.5"
          y="0.5"
          width="27"
          height="27"
          rx="7"
          fill="var(--paper)"
          stroke="var(--line-strong)"
        />
      )}

      <path
        d="M5 18.6 L11 16.9 L16 17.9 L22 16.2"
        stroke="var(--action)"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
        opacity="0.35"
      />

      {dots.map((d) => (
        <circle key={d.cx} cx={d.cx} cy={d.cy} r="1.9" fill="var(--action)" />
      ))}

      <circle cx="19" cy="8" r="3" fill="var(--risk-high)" />
      <circle
        cx="19"
        cy="8"
        r="5.5"
        stroke="var(--risk-high)"
        strokeWidth="1"
        opacity="0.28"
      />
    </svg>
  );
}