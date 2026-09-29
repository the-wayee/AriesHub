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
          AriesHub
        </Link>
        <nav aria-label="主导航">
          <Link href="/cases">案例</Link>
          <Link href="/community">讨论</Link>
          <Link href="/my-content">我的内容</Link>
          <Link href="/#about">关于</Link>
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
        AriesHub
      </Link>
      <p>与 AI 一起，实践更大的可能。</p>
      <a href="#main">回到顶部 ↑</a>
    </footer>
  );
}
