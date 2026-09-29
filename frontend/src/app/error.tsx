"use client";

import { Button } from "@/components/ui/button";

export default function ErrorPage({ retry }: { retry: () => void }) {
  return (
    <div className="content-state" role="alert">
      <h2>页面暂时无法打开</h2>
      <p>请重新尝试加载。</p>
      <Button className="primary-link" onClick={() => retry()}>
        重新加载
      </Button>
    </div>
  );
}
