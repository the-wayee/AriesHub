"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { authRequest, type CurrentUser } from "@/lib/auth";

/** 头部只获取最小登录信息；失败时回退为登录入口，不阻塞页面主体。 */
export function AuthNav() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);

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
