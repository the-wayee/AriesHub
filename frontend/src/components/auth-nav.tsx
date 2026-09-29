"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { authRequest, type CurrentUser } from "@/lib/auth";

/** 用服务端可见的会话 Cookie 决定首帧，随后向 /me 核实用户信息。 */
export function AuthNav({ initialHasSession }: { initialHasSession: boolean }) {
  const [user, setUser] = useState<CurrentUser | null | undefined>(
    initialHasSession ? undefined : null,
  );

  useEffect(() => {
    let active = true;
    const refresh = () => {
      void authRequest<CurrentUser>("/me").then((result) => {
        if (active) setUser(result.ok ? result.data : null);
      });
    };
    refresh();
    window.addEventListener("arieshub:auth", refresh);
    return () => {
      active = false;
      window.removeEventListener("arieshub:auth", refresh);
    };
  }, []);

  if (user === undefined) {
    return <span className="auth-nav-placeholder" aria-hidden="true" />;
  }
  if (!user) {
    return (
      <div className="auth-nav">
        <Link href="/login">登录</Link>
        <Link className="header-register" href="/register">
          注册
        </Link>
      </div>
    );
  }
  return (
    <div className="signed-in-nav">
      {user.role === "ADMIN" && <Link href="/admin">内容后台</Link>}
      <Link className="member-link" href="/account" title={user.email}>
        <span>{user.nickname.slice(0, 1)}</span>
        {user.nickname}
      </Link>
    </div>
  );
}
