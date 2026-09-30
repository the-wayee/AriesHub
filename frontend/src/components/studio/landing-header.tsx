"use client";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { ArrowUpRight, Asterisk, Compass, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AuthNav } from "@/components/auth-nav";
import { useAuthSession } from "@/components/auth-session";
export function LandingHeader() {
  const { hasSession } = useAuthSession();
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) return;
    const close = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        trigger.current?.focus();
      }
    };
    const outside = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("keydown", close);
    document.addEventListener("pointerdown", outside);
    return () => {
      document.removeEventListener("keydown", close);
      document.removeEventListener("pointerdown", outside);
    };
  }, [open]);
  return (
    <header className="cosmos-header">
      <Link className="cosmos-brand" href="/">
        <Asterisk aria-hidden="true" />
        AriesHub
      </Link>
      <nav className="cosmos-nav" aria-label="主导航">
        <a href="#explore">探索</a>
        <a href="#about">关于社区</a>
      </nav>
      <div className={`discovery-island ${open ? "is-open" : ""}`} ref={root}>
        <Button
          ref={trigger}
          variant="ghost"
          className="island-trigger"
          aria-expanded={open}
          aria-controls="island-content"
          onClick={() => setOpen(!open)}
        >
          <span className="island-pulse" />
          <span>{open ? "从一个好奇开始" : "今天，想探索什么？"}</span>
          {open ? <X size={16} /> : <Compass size={16} />}
        </Button>
        {open && (
          <div id="island-content" className="island-content">
            <p>选择你的下一站</p>
            {[
              ["#explore", "寻找实践灵感"],
              ["#stories", "读一篇深度文章"],
              ["#community", "看看大家聊什么"],
            ].map(([href, label]) => (
              <a key={href} href={href} onClick={() => setOpen(false)}>
                {label}
                <ArrowUpRight size={16} />
              </a>
            ))}
            <Link
              href={hasSession ? "/home" : "/register"}
              onClick={() => setOpen(false)}
            >
              {hasSession ? "回到社区" : "加入这场探索"}
              <Asterisk size={16} />
            </Link>
          </div>
        )}
      </div>
      <AuthNav />
    </header>
  );
}
