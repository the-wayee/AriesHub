"use client";

import Link from "next/link";
import { useAuthSession } from "@/components/auth-session";

/** 用服务端可见的会话 Cookie 决定首帧，随后向 /me 核实用户信息。 */
export function AuthNav() {
  const { user } = useAuthSession();

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
      <Link className="member-link" href="/home" title={user.email}>
        <span>{user.nickname.slice(0, 1)}</span>
        {user.nickname}
      </Link>
    </div>
  );
}
