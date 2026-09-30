import Link from "next/link";

export function ContentState({
  title,
  message,
  href = "/publications",
  label = "返回案例库",
  requestId,
  reload = false,
}: {
  title: string;
  message: string;
  href?: string;
  label?: string;
  requestId?: string;
  reload?: boolean;
}) {
  const Action = reload ? "a" : Link;
  return (
    <div className="content-state" role="status">
      <p className="eyebrow">ARIESHUB / LIBRARY</p>
      <h2>{title}</h2>
      <p>{message}</p>
      <Action className="text-link" href={href}>
        {label} →
      </Action>
      {requestId && <small>问题编号：{requestId}</small>}
    </div>
  );
}
