"use client";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { PageMotion } from "@/components/page-motion";
import { AuthSessionProvider } from "@/components/auth-session";
import { LandingHeader } from "./landing-header";
import { CommunityShell } from "@/components/community/community-shell";
import { SessionGate } from "@/components/session-gate";
import { isProtectedRoute } from "@/lib/protected-routes";
import { AdminShell } from "@/components/admin-shell";
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
  const auth = path === "/login" || path === "/register";
  const community = [
    "/home",
    "/discover",
    "/publications",
    "/learn",
    "/checkout",
    "/community",
    "/my-content",
    "/members",
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
      {isProtectedRoute(path) ? <SessionGate>{content}</SessionGate> : content}
    </AuthSessionProvider>
  );
}
