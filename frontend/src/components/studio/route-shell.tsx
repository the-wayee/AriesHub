"use client";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { PageMotion } from "@/components/page-motion";
import { LandingHeader } from "./landing-header";
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
  if (path === "/" || auth)
    return (
      <div className="cosmos-site">
        <a className="skip-link" href="#main">
          跳到主要内容
        </a>
        {!auth && <LandingHeader hasSession={hasSession} />}
        <main id="main">{children}</main>
      </div>
    );
  return (
    <>
      {header}
      <main id="main" className="wrap">
        <PageMotion>{children}</PageMotion>
      </main>
      {footer}
    </>
  );
}
