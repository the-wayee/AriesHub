"use client";
import Link from "next/link";
import { redirect, usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { useAuthSession } from "./auth-session";
import { PageSkeleton } from "./page-skeleton";

export function SessionGate({ children }: { children: ReactNode }) {
  const { user, sessionError } = useAuthSession();
  const path = usePathname();
  if (sessionError)
    return (
      <section className="session-state" role="alert">
        <h1>暂时无法验证登录状态</h1>
        <p>{sessionError}</p>
        <button
          onClick={() => window.dispatchEvent(new Event("arieshub:auth"))}
        >
          重新验证
        </button>
      </section>
    );
  if (user === undefined)
    return (
      <div className="session-loading">
        <PageSkeleton
          variant={
            path.startsWith("/admin/publications/")
              ? "editor"
              : path.startsWith("/admin")
                ? "dashboard"
                : "gallery"
          }
          label="正在验证登录状态"
        />
      </div>
    );
  if (!user) redirect(`/login?next=${encodeURIComponent(path)}`);
  if (path.startsWith("/admin") && user.role !== "ADMIN")
    return (
      <section className="session-state">
        <h1>此页面仅对管理员开放</h1>
        <Link href="/home">返回社区</Link>
      </section>
    );
  return children;
}
