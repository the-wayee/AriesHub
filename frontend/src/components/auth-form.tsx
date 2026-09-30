"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { authRequest, type CurrentUser } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface CodeDispatchResult {
  expiresInSeconds: number;
  resendAfterSeconds: number;
}

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [codePending, setCodePending] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [codeMessage, setCodeMessage] = useState("");
  const [error, setError] = useState("");
  const [errorRequestId, setErrorRequestId] = useState("");
  const emailRef = useRef<HTMLInputElement>(null);
  const isRegister = mode === "register";

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setInterval(() => {
      setCooldown((seconds) => (seconds > 0 ? seconds - 1 : 0));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  async function sendCode() {
    const emailInput = emailRef.current;
    if (
      !emailInput ||
      !emailInput.reportValidity() ||
      codePending ||
      cooldown > 0
    )
      return;
    setCodePending(true);
    setError("");
    setErrorRequestId("");
    setCodeMessage("");
    const result = await authRequest<CodeDispatchResult>("/email-codes", {
      method: "POST",
      body: JSON.stringify({
        email: emailInput.value.trim(),
        purpose: "REGISTER",
      }),
    });
    setCodePending(false);
    if (!result.ok) {
      setError(result.error.message);
      setErrorRequestId(result.error.requestId ?? "");
      return;
    }
    setCooldown(result.data.resendAfterSeconds);
    setCodeMessage("验证码已发送，请检查邮箱；10 分钟内有效。");
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError("");
    setErrorRequestId("");
    const values = new FormData(event.currentTarget);
    const body = {
      email: String(values.get("email") ?? "").trim(),
      password: String(values.get("password") ?? ""),
      ...(isRegister
        ? {
            nickname: String(values.get("nickname") ?? "").trim(),
            code: String(values.get("code") ?? "").trim(),
          }
        : {}),
    };
    const result = await authRequest<CurrentUser>(
      isRegister ? "/register" : "/login",
      { method: "POST", body: JSON.stringify(body) },
    );
    setPending(false);
    if (!result.ok) {
      setError(result.error.message);
      setErrorRequestId(result.error.requestId ?? "");
      return;
    }
    window.dispatchEvent(
      new CustomEvent("arieshub:auth", { detail: result.data }),
    );
    router.push("/account");
    router.refresh();
  }

  return (
    <form className="auth-form" onSubmit={submit}>
      {isRegister && (
        <div className="form-field">
          <Label htmlFor="nickname">怎么称呼你</Label>
          <Input
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
        <Label htmlFor="email">邮箱</Label>
        <Input
          id="email"
          ref={emailRef}
          name="email"
          type="email"
          autoComplete="email"
          maxLength={254}
          required
          placeholder="you@example.com"
        />
      </div>
      {isRegister && (
        <div className="form-field">
          <Label htmlFor="code">邮箱验证码</Label>
          <div className="code-field">
            <Input
              id="code"
              name="code"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              minLength={6}
              maxLength={6}
              required
              placeholder="6 位验证码"
            />
            <Button
              variant="outline"
              className="code-button"
              type="button"
              disabled={codePending || cooldown > 0}
              onClick={sendCode}
            >
              {codePending
                ? "发送中…"
                : cooldown > 0
                  ? `${cooldown}s 后重发`
                  : "获取验证码"}
            </Button>
          </div>
          {codeMessage && <small className="form-success">{codeMessage}</small>}
        </div>
      )}
      <div className="form-field">
        <Label htmlFor="password">密码</Label>
        <Input
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
          {errorRequestId && <small>请求编号：{errorRequestId}</small>}
        </p>
      )}
      <Button
        className="primary-link auth-submit"
        type="submit"
        disabled={pending}
      >
        {pending ? "正在提交…" : isRegister ? "创建账号 ↗" : "登录 AriesHub ↗"}
      </Button>
      <p className="auth-switch">
        {isRegister ? "已经有账号？" : "第一次来这里？"}{" "}
        <Link href={isRegister ? "/login" : "/register"}>
          {isRegister ? "去登录" : "免费注册"}
        </Link>
      </p>
    </form>
  );
}
