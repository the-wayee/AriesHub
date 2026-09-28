"use client";

export default function ErrorPage({ retry }: { retry: () => void }) {
  return (
    <div className="content-state" role="alert">
      <h2>页面暂时无法打开</h2>
      <p>请重新尝试加载。</p>
      <button className="primary-link" onClick={() => retry()}>
        重新加载
      </button>
    </div>
  );
}
