"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authRequest, type CurrentUser } from "@/lib/auth";

export function AccountPanel() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);

  useEffect(() => {
    let active = true;
    void authRequest<CurrentUser>("/me").then((result) => {
      if (active) setUser(result.ok ? result.data : null);
    });
    return () => {
      active = false;
    };
  }, []);

  async function logout() {
    const result = await authRequest<void>("/logout", { method: "POST" });
    if (!result.ok) return;
    window.dispatchEvent(new Event("arieshub:auth"));
    router.push("/");
  }

  if (user === undefined) {
    return <p className="account-loading">正在读取账号…</p>;
  }
  if (!user) {
    return (
      <div className="account-empty">
        <h2>登录后查看你的账号</h2>
        <p>账号会用于保存购买记录、收藏和社区权益。</p>
        <Link className="primary-link" href="/login">
          去登录 ↗
        </Link>
      </div>
    );
  }
  return (
    <div className="account-card">
      <div className="account-avatar" aria-hidden="true">
        {user.nickname.slice(0, 1)}
      </div>
      <div>
        <p className="eyebrow">MEMBER PROFILE</p>
        <h2>{user.nickname}</h2>
        <p>{user.email}</p>
      </div>
      <dl>
        <div>
          <dt>账号角色</dt>
          <dd>{user.role === "ADMIN" ? "管理员" : "社区成员"}</dd>
        </div>
        <div>
          <dt>加入时间</dt>
          <dd>
            {new Intl.DateTimeFormat("zh-CN").format(new Date(user.createdAt))}
          </dd>
        </div>
        <div>
          <dt>邮箱状态</dt>
          <dd>{user.emailVerified ? "已验证" : "待验证"}</dd>
        </div>
      </dl>
      <div className="account-actions">
        <Link className="primary-link" href="/cases">
          浏览案例库 ↗
        </Link>
        <button className="quiet-button" type="button" onClick={logout}>
          退出登录
        </button>
      </div>
    </div>
  );
}
