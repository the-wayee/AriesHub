"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { authRequest, type CurrentUser } from "@/lib/auth";

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const isRegister = mode === "register";

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError("");
    const values = new FormData(event.currentTarget);
    const body = {
      email: String(values.get("email") ?? "").trim(),
      password: String(values.get("password") ?? ""),
      ...(isRegister
        ? { nickname: String(values.get("nickname") ?? "").trim() }
        : {}),
    };
    const result = await authRequest<CurrentUser>(
      isRegister ? "/register" : "/login",
      { method: "POST", body: JSON.stringify(body) },
    );
    setPending(false);
    if (!result.ok) {
      setError(result.error.message);
      return;
    }
    window.dispatchEvent(new Event("arieshub:auth"));
    router.push("/account");
  }

  return (
    <form className="auth-form" onSubmit={submit}>
      {isRegister && (
        <div className="form-field">
          <label htmlFor="nickname">怎么称呼你</label>
          <input
            id="nickname"
            name="nickname"
            autoComplete="nickname"
            minLength={2}
            maxLength={30}
            required
            placeholder="你的昵称"
          />
        </div>
      )}
      <div className="form-field">
        <label htmlFor="email">邮箱</label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          maxLength={254}
          required
          placeholder="you@example.com"
        />
      </div>
      <div className="form-field">
        <label htmlFor="password">密码</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete={isRegister ? "new-password" : "current-password"}
          minLength={isRegister ? 8 : undefined}
          maxLength={72}
          pattern={isRegister ? "(?=.*[A-Za-z])(?=.*[0-9]).{8,72}" : undefined}
          required
          placeholder={isRegister ? "至少 8 位，包含字母和数字" : "输入密码"}
        />
        {isRegister && (
          <small>使用 8–72 位字符，至少包含一个字母和一个数字。</small>
        )}
      </div>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <button
        className="primary-link auth-submit"
        type="submit"
        disabled={pending}
      >
        {pending ? "正在提交…" : isRegister ? "创建账号 ↗" : "登录 AriesHub ↗"}
      </button>
      <p className="auth-switch">
        {isRegister ? "已经有账号？" : "第一次来这里？"}{" "}
        <Link href={isRegister ? "/login" : "/register"}>
          {isRegister ? "去登录" : "免费注册"}
        </Link>
      </p>
    </form>
  );
}
