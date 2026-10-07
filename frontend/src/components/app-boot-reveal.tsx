"use client";

import { useId, useRef, useState, type ReactNode } from "react";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";
import { AnimatedCaption } from "./animated-caption";
import { getRandomQuote, type RandomQuote } from "@/lib/random-quote";

gsap.registerPlugin(useGSAP);

const ARIES_PATH =
  "M64 104V48C64 29 51 19 37 21C21 23 16 38 23 49C28 57 42 59 46 47M64 48C64 29 77 19 91 21C107 23 112 38 105 49C100 57 86 59 82 47";

/** 整页打开/刷新及公开入口进入主页时播放；普通站内导航沿用完成状态。 */
export function AppBootReveal({ children }: { children: ReactNode }) {
  const root = useRef<HTMLDivElement>(null);
  const content = useRef<HTMLDivElement>(null);
  const maskId = useId();
  const [active, setActive] = useState(true);
  const [quote, setQuote] = useState<RandomQuote | null>(null);

  useGSAP(
    (_context, contextSafe) => {
      const surface = content.current;
      if (!surface || !contextSafe) return;
      const reduced = window.matchMedia(
        "(prefers-reduced-motion: reduce)",
      ).matches;
      const previousOverflow = document.documentElement.style.overflow;
      document.documentElement.style.overflow = "hidden";
      surface.inert = true;
      let filled = false;
      let finishing = false;
      let frame = 0;
      let quoteFrame = 0;
      let mounted = true;

      const finish = contextSafe(() => {
        if (finishing) return;
        finishing = true;
        // 填满、轻微舒展、页面显现相互重叠，不替换图形或突然切屏。
        const timeline = gsap
          .timeline({
            onComplete: () => {
              surface.inert = false;
              document.documentElement.style.overflow = previousOverflow;
              setActive(false);
            },
          })
          .to(".app-boot-fill", {
            attr: { y: 0, height: 128 },
            duration: reduced ? 0 : 0.35,
            ease: "power2.out",
          })
          .to(
            ".app-boot-symbol",
            {
              scale: reduced ? 1 : 1.045,
              duration: reduced ? 0 : 0.45,
              ease: "sine.out",
            },
            0,
          );
        timeline
          .to(
            surface,
            { opacity: 1, duration: reduced ? 0.12 : 0.6 },
            reduced ? 0 : 0.15,
          )
          .to(
            ".app-boot-overlay",
            {
              opacity: 0,
              duration: reduced ? 0.12 : 0.6,
              ease: "power2.inOut",
            },
            "<",
          );
      });
      const checkReady = () => {
        cancelAnimationFrame(frame);
        if (
          !filled ||
          finishing ||
          document.readyState !== "complete" ||
          surface.querySelector('.page-skeleton[aria-busy="true"]')
        )
          return;
        // 给流式内容和认证后首个请求两帧提交时间，避免先露出骨架再显示正文。
        frame = requestAnimationFrame(() => {
          frame = requestAnimationFrame(() => {
            if (!surface.querySelector('.page-skeleton[aria-busy="true"]'))
              finish();
          });
        });
      };
      // 未就绪时停留在将满状态；这是加载图形，不伪装成网络百分比。
      gsap.to(".app-boot-fill", {
        attr: { y: 24, height: 104 },
        duration: reduced ? 0.12 : 1.25,
        ease: "power1.inOut",
        onComplete: () => {
          filled = true;
          checkReady();
        },
      });
      const observer = new MutationObserver(checkReady);
      observer.observe(surface, { childList: true, subtree: true });
      window.addEventListener("load", checkReady);
      // 文案到达即显现，与图标填充并行；不额外等待文案或增加停留时间。
      const revealQuote = contextSafe(() => {
        gsap
          .timeline()
          .to(".animated-caption-character", {
            opacity: 1,
            y: 0,
            duration: reduced ? 0.12 : 0.4,
            stagger: reduced ? 0 : { amount: 0.2 },
            ease: "power2.out",
          })
          .to(
            ".app-boot-quote-source",
            {
              opacity: 1,
              y: 0,
              duration: reduced ? 0.12 : 0.3,
            },
            "<0.12",
          );
      });
      void getRandomQuote().then((result) => {
        if (!mounted || finishing || !result) return;
        setQuote(result);
        quoteFrame = requestAnimationFrame(() => {
          quoteFrame = requestAnimationFrame(() => {
            if (mounted && !finishing) revealQuote();
          });
        });
      });
      return () => {
        mounted = false;
        observer.disconnect();
        window.removeEventListener("load", checkReady);
        cancelAnimationFrame(frame);
        cancelAnimationFrame(quoteFrame);
        surface.inert = false;
        document.documentElement.style.overflow = previousOverflow;
      };
    },
    { scope: root },
  );

  return (
    <div ref={root} className="app-boot">
      {active && (
        <div
          className="app-boot-overlay"
          role="status"
          aria-label="正在加载页面"
        >
          <div className="app-boot-mark">
            <svg
              className="app-boot-symbol"
              viewBox="0 0 128 128"
              aria-hidden="true"
            >
              <defs>
                <clipPath id={maskId}>
                  <rect
                    className="app-boot-fill"
                    x="0"
                    y="128"
                    width="128"
                    height="0"
                  />
                </clipPath>
              </defs>
              <path className="app-boot-outline" d={ARIES_PATH} />
              <path
                className="app-boot-color"
                d={ARIES_PATH}
                clipPath={`url(#${maskId})`}
              />
            </svg>
            {quote && (
              <div className="app-boot-quote">
                <AnimatedCaption text={quote.text} />
                <span className="app-boot-quote-source">
                  {quote.source ? `— ${quote.source}` : "一言"}
                </span>
              </div>
            )}
          </div>
        </div>
      )}
      <div ref={content} className="app-boot-content">
        {children}
      </div>
      <noscript>
        <style>{`.app-boot-overlay{display:none}.app-boot-content{opacity:1}`}</style>
      </noscript>
    </div>
  );
}
