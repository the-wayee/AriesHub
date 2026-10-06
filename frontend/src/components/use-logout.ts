"use client";
import { useState } from "react";
import { authRequest } from "@/lib/auth";

export function useLogout() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function logout() {
    if (busy) return;
    setBusy(true);
    setError("");
    const result = await authRequest<void>("/logout", { method: "POST" });
    if (!result.ok) {
      setError(result.error.msg);
      setBusy(false);
      return;
    }
    window.dispatchEvent(new CustomEvent("arieshub:auth", { detail: null }));
    // 退出会话后重新建立访客文档，清空旧路由缓存，避免登录守卫与首页导航竞争。
    window.location.replace("/");
    setBusy(false);
  }
  return { logout, busy, error };
}
