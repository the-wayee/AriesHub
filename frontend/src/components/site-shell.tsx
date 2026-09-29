import Link from "next/link";
import { cookies } from "next/headers";
import { AuthNav } from "@/components/auth-nav";

export async function SiteHeader() {
  const hasSession = (await cookies()).has("arieshub_token");

  return (
    <>
      <a className="skip-link" href="#main">
        跳到主要内容
      </a>
      <header className="site-header wrap">
        <Link className="brand" href="/" aria-label="AriesHub 首页">
          <span className="brand-mark" aria-hidden="true">
            a.
          </span>
          AriesHub
        </Link>
        <nav aria-label="主导航">
          <Link href="/cases">案例库</Link>
          <Link href="/#how-it-works">如何使用</Link>
          <Link href="/#about">关于这里</Link>
        </nav>
        <AuthNav initialHasSession={hasSession} />
      </header>
    </>
  );
}

export function SiteFooter() {
  return (
    <footer className="site-footer wrap">
      <Link className="brand" href="/">
        AriesHub<span className="accent">.</span>
      </Link>
      <p>保持好奇，持续动手。</p>
      <a href="#main">回到顶部 ↑</a>
    </footer>
  );
}
