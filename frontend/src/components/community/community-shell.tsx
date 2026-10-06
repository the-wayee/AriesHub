"use client";

import {
  Asterisk,
  Bell,
  Bookmark,
  Compass,
  MessageCircle,
  Search,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { type ReactNode } from "react";
import { PageMotion } from "@/components/page-motion";
import { AuthNav } from "@/components/auth-nav";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const navigation = [
  { href: "/home", label: "首页", icon: Compass },
  { href: "/discover", label: "探索", icon: Search },
  { href: "/community", label: "讨论", icon: MessageCircle },
  { href: "/my-content", label: "我的空间", icon: Bookmark },
];

export function CommunityShell({ children }: { children: ReactNode }) {
  const path = usePathname();

  const selected = (href: string) => {
    if (href === "/home") return path === "/home";
    if (href === "/discover")
      return ["/discover", "/publications", "/learn", "/checkout"].some(
        (prefix) => path === prefix || path.startsWith(`${prefix}/`),
      );
    return path === href || path.startsWith(`${href}/`);
  };

  return (
    <div
      className={`cosmos-site community-hub${path === "/account" ? " profile-shell" : ""}`}
    >
      <a className="skip-link" href="#main">
        跳到主要内容
      </a>
      <header className="hub-header">
        <Link className="hub-brand" href="/">
          <Asterisk aria-hidden="true" />
          AriesHub
        </Link>
        <form action="/discover" className="hub-island" role="search">
          <span className="hub-live-dot" aria-hidden="true" />
          <Input
            aria-label="搜索内容"
            name="q"
            maxLength={120}
            placeholder="今天，社区里发生了什么？"
          />
          <Button variant="ghost" size="icon" type="submit" aria-label="搜索">
            <Search />
          </Button>
        </form>
        <div className="hub-header-actions">
          <Button variant="ghost" size="icon" aria-label="通知预览" disabled>
            <Bell />
          </Button>
          <AuthNav />
        </div>
      </header>
      {path !== "/account" && (
        <nav className="hub-nav" aria-label="社区导航">
          {navigation.map(({ href, label, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              aria-current={selected(href) ? "page" : undefined}
            >
              <Icon aria-hidden="true" />
              {label}
            </Link>
          ))}
        </nav>
      )}
      <main id="main" className="hub-main">
        <PageMotion>{children}</PageMotion>
      </main>
      <footer className="hub-footer">
        <Link href="/">AriesHub</Link>
        <span>一起探索，保持好奇。</span>
        {path !== "/account" && (
          <small>
            {/^\/publications\/[1-9]\d*$/.test(path)
              ? "互动仅保存在当前浏览器"
              : "社区预览 · 互动仅保存在当前浏览器"}
          </small>
        )}
      </footer>
    </div>
  );
}
