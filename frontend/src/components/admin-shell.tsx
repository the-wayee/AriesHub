"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import {
  Asterisk,
  ArrowUpRight,
  LayoutDashboard,
  FileText,
  Users,
  BarChart3,
  MessageSquare,
  Coins,
  Menu,
  X,
  Sparkles,
  Tags,
} from "lucide-react";
import { AuthNav } from "./auth-nav";
import { PageMotion } from "./page-motion";
const links = [
  { href: "/admin", label: "工作台", icon: LayoutDashboard },
  { href: "/admin/publications", label: "内容管理", icon: FileText },
  { href: "/admin/categories", label: "分类管理", icon: Tags },
  { href: "/admin/users", label: "成员管理", icon: Users },
  { href: "/admin/comments", label: "评论与审核", icon: MessageSquare },
  { href: "/admin/analytics", label: "数据分析", icon: BarChart3 },
  { href: "/admin/credits", label: "积分与权益", icon: Coins },
];
export function AdminShell({ children }: { children: ReactNode }) {
  const path = usePathname();
  const [open, setOpen] = useState(false);
  useEffect(() => {
    const close = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, []);
  const current = links.find((x) =>
    x.href === "/admin" ? path === x.href : path.startsWith(x.href),
  );
  return (
    <div
      className={`cosmos-site admin-studio admin-app ${open ? "admin-nav-open" : ""}`}
    >
      <a className="skip-link" href="#main">
        跳到主要内容
      </a>
      {open && (
        <button
          className="admin-nav-backdrop"
          aria-label="关闭后台导航"
          onClick={() => setOpen(false)}
        />
      )}
      <aside className="admin-sidebar" aria-label="后台侧栏">
        <Link
          className="admin-brand"
          href="/admin"
          onClick={() => setOpen(false)}
        >
          <Asterisk size={26} />
          <span>
            AriesHub<small>运营工作台</small>
          </span>
        </Link>
        <div className="admin-space">
          <span className="space-monogram">A</span>
          <div>
            AI 实践社区<small>内容 · 成员 · 成长</small>
          </div>
        </div>
        <p className="sidebar-label">管理空间</p>
        <nav aria-label="后台导航">
          {links.map((x) => (
            <Link
              key={x.href}
              href={x.href}
              aria-current={
                (
                  x.href === "/admin"
                    ? path === x.href
                    : path.startsWith(x.href)
                )
                  ? "page"
                  : undefined
              }
              onClick={() => setOpen(false)}
            >
              <x.icon size={18} />
              {x.label}
            </Link>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <Sparkles size={19} />
          <p>
            好的内容，持续生长。<small>案例、学习文章与课程</small>
          </p>
          <Link href="/home">
            回到社区 <ArrowUpRight size={14} />
          </Link>
        </div>
      </aside>
      <div className="admin-workspace">
        <header className="admin-header">
          <div className="admin-breadcrumb">
            <button
              className="admin-mobile-toggle"
              onClick={() => setOpen(!open)}
              aria-expanded={open}
              aria-label="展开后台导航"
            >
              {open ? <X size={20} /> : <Menu size={20} />}
            </button>
            <span>管理空间</span>
            <span>/</span>
            <strong>{current?.label ?? "内容创作"}</strong>
          </div>
          <div className="admin-header-actions">
            <Link className="admin-community-link" href="/home">
              查看社区 <ArrowUpRight size={14} />
            </Link>
            <AuthNav />
          </div>
        </header>
        <main id="main" className="studio-main admin-main">
          <PageMotion>{children}</PageMotion>
        </main>
        <footer className="admin-bottom-note">
          AriesHub · 付费 AI 实践社区 <span>内容的价值，来自真实的实践。</span>
        </footer>
      </div>
    </div>
  );
}
