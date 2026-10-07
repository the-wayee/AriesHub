"use client";

import {
  Asterisk,
  Bell,
  Bookmark,
  Compass,
  MessageCircle,
  Search,
  Plus,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { type ReactNode } from "react";
import { PageMotion } from "@/components/page-motion";
import { AuthNav } from "@/components/auth-nav";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuthSession } from "@/components/auth-session";

const navigation = [
  { href: "/home", label: "首页", icon: Compass },
  { href: "/discover", label: "探索", icon: Search },
  { href: "/community", label: "讨论", icon: MessageCircle },
  { href: "/my-content", label: "我的空间", icon: Bookmark },
];

export function CommunityShell({ children }: { children: ReactNode }) {
  const path = usePathname();
  const { user } = useAuthSession();
  const home = path === "/home";

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
      className={`cosmos-site community-hub${home ? " community-home-shell" : ""}${path === "/account" ? " profile-shell" : ""}`}
    >
      <a className="skip-link" href="#main">
        跳到主要内容
      </a>
      <header className="hub-header">
        <Link className="hub-brand" href="/">
          <Asterisk aria-hidden="true" />
          AriesHub
        </Link>
        {home && (
          <nav className="home-header-nav" aria-label="社区导航">
            {navigation.map(({ href, label }) => (
              <Link
                key={href}
                href={href}
                aria-current={selected(href) ? "page" : undefined}
              >
                {label}
              </Link>
            ))}
          </nav>
        )}
        <form action="/discover" className="hub-island" role="search">
          <span className="hub-live-dot" aria-hidden="true" />
          <Input
            aria-label="搜索内容"
            name="q"
            maxLength={120}
            placeholder="搜索文章、案例和课程"
          />
          <Button variant="ghost" size="icon" type="submit" aria-label="搜索">
            <Search />
          </Button>
        </form>
        <div className="hub-header-actions">
          {home && (
            <Link
              className="home-create-link"
              href={
                user?.role === "ADMIN"
                  ? "/admin/publications/new"
                  : "/community"
              }
            >
              <Plus aria-hidden="true" />
              {user?.role === "ADMIN" ? "创建内容" : "参与讨论"}
            </Link>
          )}
          <Button variant="ghost" size="icon" aria-label="通知预览" disabled>
            <Bell />
          </Button>
          <AuthNav />
        </div>
      </header>
      {path !== "/account" && !home && (
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
            {["/home", "/discover", "/my-content"].includes(path) ||
            /^\/publications\/[1-9]\d*$/.test(path)
              ? "收藏、点赞与阅读记录随账号保存"
              : "讨论及旧版阅读器仍为社区预览"}
          </small>
        )}
      </footer>
    </div>
  );
}
