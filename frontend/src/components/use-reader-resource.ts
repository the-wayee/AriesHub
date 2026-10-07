"use client";
import { useEffect, useState } from "react";
import { useAuthSession } from "./auth-session";
import { apiRequest } from "@/lib/api";
/** 路径与刷新编号共同标记快照，慢的旧筛选响应不能覆盖新结果。 */
export function useReaderResource<T>(path: string | null, revision = 0) {
  const [snapshot, setSnapshot] = useState<{
    key: string;
    data?: T;
    error?: string;
  } | null>(null);
  const { user, hasSession } = useAuthSession();
  const effectivePath = hasSession && user === undefined ? null : path;
  const key =
    effectivePath === null
      ? null
      : `${effectivePath}:${revision}:${user?.id ?? "anon"}`;
  useEffect(() => {
    if (!effectivePath || !key) return;
    let active = true;
    void apiRequest<T>(effectivePath, { cache: "no-store" }).then((r) => {
      if (active)
        setSnapshot(r.ok ? { key, data: r.data } : { key, error: r.error.msg });
    });
    return () => {
      active = false;
    };
  }, [effectivePath, key]);
  return key !== null && snapshot?.key === key
    ? snapshot
    : { data: undefined, error: undefined };
}
