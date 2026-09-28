import type { Metadata } from "next";
import { AuthForm } from "@/components/auth-form";

export const metadata: Metadata = { title: "注册" };

export default function RegisterPage() {
  return (
    <div className="auth-page">
      <section className="auth-intro">
        <p className="eyebrow">JOIN THE MAKERS</p>
        <h1>
          从一个案例，
          <br />
          开始一次实践。
        </h1>
        <p>注册账号，逐步解锁案例素材、学习记录与社区内容。</p>
      </section>
      <section className="auth-box" aria-labelledby="register-title">
        <p className="eyebrow">ARIESHUB / CREATE ACCOUNT</p>
        <h2 id="register-title">加入 AriesHub</h2>
        <AuthForm mode="register" />
      </section>
    </div>
  );
}
