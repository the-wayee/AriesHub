"use client";
import Link from "next/link";
import { Asterisk, ArrowUpRight } from "lucide-react";
import { AuthNav } from "./auth-nav";
import type { ReactNode } from "react";

export function AdminShell({ children }: { children: ReactNode }) {
  return (
    <div className="cosmos-site admin-studio">
      <a className="skip-link" href="#main">
        跳到主要内容
      </a>
      <header className="studio-header">
        <Link className="studio-brand" href="/">
          <Asterisk aria-hidden="true" /> AriesHub <span>Studio</span>
        </Link>
        <nav aria-label="后台导航">
          <Link href="/admin">内容</Link>
          <Link href="/home">
            回到社区 <ArrowUpRight aria-hidden="true" />
          </Link>
        </nav>
        <AuthNav />
      </header>
      <main id="main" className="studio-main">
        {children}
      </main>
      <footer className="studio-footer">
        <span>AriesHub / Studio</span>
        <span>记录实践，整理内容。</span>
      </footer>
    </div>
  );
}
