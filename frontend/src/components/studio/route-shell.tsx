"use client";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import { PageMotion } from "@/components/page-motion";
import { AuthSessionProvider } from "@/components/auth-session";
import { LandingHeader } from "./landing-header";
import { CommunityShell } from "@/components/community/community-shell";
import { SessionGate } from "@/components/session-gate";
import { isProtectedRoute } from "@/lib/protected-routes";
import { AdminShell } from "@/components/admin-shell";
import { AppBootReveal } from "@/components/app-boot-reveal";
export function RouteShell({
  children,
  header,
  footer,
  hasSession,
}: {
  children: ReactNode;
  header: ReactNode;
  footer: ReactNode;
  hasSession: boolean;
}) {
  const path = usePathname();
  const [entrance, setEntrance] = useState({ path, cycle: 0 });
  if (entrance.path !== path) {
    // 公开页/登录注册进入社区是一次品牌入场，社区内部导航不重复遮罩。
    // 在渲染中更新路径快照，让新的遮罩和目标页同次提交，避免先露出页面。
    const entersHome =
      path === "/home" && ["/", "/login", "/register"].includes(entrance.path);
    setEntrance({ path, cycle: entrance.cycle + (entersHome ? 1 : 0) });
  }
  const auth = path === "/login" || path === "/register";
  const community = [
    "/home",
    "/discover",
    "/publications",
    "/preview",
    "/learn",
    "/checkout",
    "/community",
    "/my-content",
    "/account",
  ].some((prefix) => path === prefix || path.startsWith(`${prefix}/`));
  let content: ReactNode;
  if (path === "/admin" || path.startsWith("/admin/")) {
    content = <AdminShell>{children}</AdminShell>;
  } else if (path === "/" || auth) {
    content = (
      <div className="cosmos-site">
        <a className="skip-link" href="#main">
          跳到主要内容
        </a>
        {!auth && <LandingHeader />}
        <main id="main">
          <PageMotion>{children}</PageMotion>
        </main>
      </div>
    );
  } else if (path.startsWith("/s/")) {
    content = (
      <main id="main" className="shared-publication-shell">
        <PageMotion>{children}</PageMotion>
      </main>
    );
  } else if (community) {
    content = <CommunityShell>{children}</CommunityShell>;
  } else {
    content = (
      <>
        {header}
        <main id="main" className="wrap">
          <PageMotion>{children}</PageMotion>
        </main>
        {footer}
      </>
    );
  }
  return (
    <AuthSessionProvider initialHasSession={hasSession}>
      <AppBootReveal key={entrance.cycle}>
        {isProtectedRoute(path) ? (
          <SessionGate>{content}</SessionGate>
        ) : (
          content
        )}
      </AppBootReveal>
    </AuthSessionProvider>
  );
}
