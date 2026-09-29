"use client";

import {
  useEffect,
  useRef,
  type ComponentPropsWithoutRef,
  type ReactNode,
} from "react";
import { usePathname } from "next/navigation";
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

gsap.registerPlugin(ScrollTrigger);

function useReveal(
  ref: React.RefObject<HTMLElement | null>,
  mode: "load" | "scroll",
  direction: "left" | "right" | "up" | "scale",
  delay = 0,
) {
  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const media = gsap.matchMedia();
    media.add(
      "(prefers-reduced-motion: no-preference)",
      () => {
        const horizontal = direction === "left" || direction === "right";
        gsap.from(element, {
          opacity: 0,
          x: horizontal ? (direction === "left" ? -72 : 72) : 0,
          y: horizontal ? 0 : direction === "scale" ? 16 : 36,
          scale: direction === "scale" ? 0.94 : 1,
          rotation: mode === "load" && direction === "right" ? 6 : 0,
          duration: mode === "load" ? 0.88 : 0.82,
          delay,
          ease: "power3.out",
          clearProps: "transform,opacity,willChange",
          willChange: "transform,opacity",
          scrollTrigger:
            mode === "scroll"
              ? {
                  trigger: element,
                  start: "top 88%",
                  once: true,
                }
              : undefined,
        });
      },
      element,
    );

    return () => media.revert();
  }, [delay, direction, mode, ref]);
}

export function PageMotion({ children }: { children: ReactNode }) {
  const root = useRef<HTMLDivElement>(null);
  const pathname = usePathname();

  useEffect(() => {
    const element = root.current;
    if (!element) return;

    const media = gsap.matchMedia();
    media.add(
      "(prefers-reduced-motion: no-preference)",
      () => {
        gsap.fromTo(
          element,
          { opacity: 0.86, x: pathname === "/" ? -18 : 24 },
          {
            opacity: 1,
            x: 0,
            duration: 0.66,
            ease: "power2.out",
            clearProps: "transform,opacity,willChange",
            willChange: "transform,opacity",
          },
        );
      },
      element,
    );

    return () => media.revert();
  }, [pathname]);

  return (
    <div ref={root} className="page-motion-root">
      {children}
    </div>
  );
}

type MotionSectionProps = ComponentPropsWithoutRef<"section"> & {
  mode?: "load" | "scroll";
  direction?: "left" | "right" | "up" | "scale";
  delay?: number;
};

export function MotionSection({
  mode = "scroll",
  direction = "up",
  delay = 0,
  ...props
}: MotionSectionProps) {
  const ref = useRef<HTMLElement>(null);
  useReveal(ref, mode, direction, delay);
  return <section ref={ref} {...props} />;
}

type MotionArticleProps = ComponentPropsWithoutRef<"article"> & {
  direction?: "left" | "right" | "up" | "scale";
  delay?: number;
};

export function MotionArticle({
  direction = "up",
  delay = 0,
  ...props
}: MotionArticleProps) {
  const ref = useRef<HTMLElement>(null);
  useReveal(ref, "scroll", direction, delay);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const media = gsap.matchMedia();
    media.add(
      "(hover: hover) and (prefers-reduced-motion: no-preference)",
      () => {
        const lift = () =>
          gsap.to(element, {
            y: -8,
            scale: 1.015,
            duration: 0.32,
            ease: "power2.out",
            overwrite: "auto",
          });
        const settle = () =>
          gsap.to(element, {
            y: 0,
            scale: 1,
            duration: 0.4,
            ease: "power2.out",
            overwrite: "auto",
          });

        element.addEventListener("mouseenter", lift);
        element.addEventListener("mouseleave", settle);
        element.addEventListener("focusin", lift);
        element.addEventListener("focusout", settle);

        return () => {
          element.removeEventListener("mouseenter", lift);
          element.removeEventListener("mouseleave", settle);
          element.removeEventListener("focusin", lift);
          element.removeEventListener("focusout", settle);
          gsap.killTweensOf(element);
        };
      },
      element,
    );

    return () => media.revert();
  }, []);

  return <article ref={ref} {...props} />;
}

export function MotionHeroCopy({ children }: { children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useReveal(ref, "load", "left");
  return <div ref={ref}>{children}</div>;
}

export function MotionHeroNote({ children }: { children: ReactNode }) {
  const ref = useRef<HTMLElement>(null);
  useReveal(ref, "load", "right", 0.16);
  return (
    <aside ref={ref} className="field-note" aria-label="AriesHub 的实践理念">
      {children}
    </aside>
  );
}

export function MotionOrbit({ secondary = false }: { secondary?: boolean }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const media = gsap.matchMedia();
    media.add("(prefers-reduced-motion: no-preference)", () => {
      gsap.to(element, {
        rotation: secondary ? "-=360" : "+=360",
        duration: secondary ? 38 : 32,
        repeat: -1,
        ease: "none",
      });
    });

    return () => media.revert();
  }, [secondary]);

  return <div ref={ref} className={`orbit${secondary ? " second" : ""}`} />;
}

export function MotionAsterisk() {
  const ref = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const media = gsap.matchMedia();
    media.add("(prefers-reduced-motion: no-preference)", () => {
      gsap.to(element, {
        scale: 1.045,
        rotation: -8,
        duration: 2.8,
        repeat: -1,
        yoyo: true,
        ease: "sine.inOut",
      });
    });

    return () => media.revert();
  }, []);

  return (
    <span ref={ref} className="asterisk">
      ✳
    </span>
  );
}
