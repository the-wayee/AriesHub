"use client";
import Image from "next/image";
import { useEffect, useRef } from "react";
import gsap from "gsap";
import { brands } from "@/lib/brand-icons";
export function BrandOrbit({ compact = false }: { compact?: boolean }) {
  const root = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const media = gsap.matchMedia();
    media.add(
      "(prefers-reduced-motion: no-preference)",
      () => {
        const cards = root.current?.querySelectorAll(".brand-tile");
        cards?.forEach((card, i) => {
          gsap.to(card, {
            y: i % 2 ? 28 : -24,
            rotationZ: `+=${i % 2 ? 14 : -12}`,
            rotationY: i % 3 ? 18 : -20,
            duration: 5 + (i % 5),
            repeat: -1,
            yoyo: true,
            ease: "sine.inOut",
            delay: -i * 0.7,
          });
        });
      },
      root,
    );
    return () => media.revert();
  }, []);
  return (
    <div
      className={`brand-orbit ${compact ? "compact" : ""}`}
      ref={root}
      aria-hidden="true"
    >
      {[...brands, ...brands.slice(0, 9)].map(([file, name, color], i) => {
        const a = (i / 30) * Math.PI * 2;
        const ring = i % 3 === 0 ? 0.78 : 1;
        return (
          <div
            className="brand-position"
            key={`${file}-${i}`}
            style={{
              left: `${(50 + 47 * Math.cos(a) * ring).toFixed(3)}%`,
              top: `${(50 + 45 * Math.sin(a)).toFixed(3)}%`,
              width: `${58 + (i % 4) * 14}px`,
              transform: `rotate(${(i % 7) * 9 - 25}deg)`,
              opacity: i % 5 === 0 ? 0.45 : 0.94,
            }}
          >
            <div className="brand-tile" style={{ background: color }}>
              <Image
                src={`/brands/${file}.svg`}
                alt={name}
                width={52}
                height={52}
              />
            </div>
          </div>
        );
      })}
      <div className="orbit-feather" />
    </div>
  );
}
