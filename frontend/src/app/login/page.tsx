import type { Metadata } from "next";
import { AuthForm } from "@/components/auth-form";

export const metadata: Metadata = { title: "登录" };

export default function LoginPage() {
  return (
    <div className="auth-page">
      <section className="auth-intro">
        <p className="eyebrow">WELCOME BACK</p>
        <h1>
          继续把想法，
          <br />
          做成作品。
        </h1>
        <p>登录后，你的购买记录、收藏与社区权益会集中保存在账号中。</p>
      </section>
      <section className="auth-box" aria-labelledby="login-title">
        <p className="eyebrow">ARIESHUB / SIGN IN</p>
        <h2 id="login-title">登录账号</h2>
        <AuthForm mode="login" />
      </section>
    </div>
  );
}
