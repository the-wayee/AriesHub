"use client";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Asterisk,
  Pause,
  Play,
  Volume2,
  VolumeX,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { BrandOrbit } from "./brand-orbit";
import { conceptCases } from "@/lib/concept-cases";
import { useAuthSession } from "@/components/auth-session";
gsap.registerPlugin(ScrollTrigger);
const topics = ["AI 编程", "内容创作", "自动化", "知识管理"];
export function Landing() {
  const { hasSession } = useAuthSession();
  const root = useRef<HTMLDivElement>(null);
  const video = useRef<HTMLVideoElement>(null);
  const [playing, setPlaying] = useState(false);
  const [muted, setMuted] = useState(true);
  const [videoError, setVideoError] = useState(false);
  const [active, setActive] = useState(0);
  useEffect(() => {
    const media = gsap.matchMedia();
    media.add(
      "(prefers-reduced-motion: no-preference)",
      () => {
        gsap.from(".hero-copy > *", {
          y: 28,
          opacity: 0,
          stagger: 0.12,
          duration: 1.2,
          ease: "power3.out",
        });
        gsap.to(".hero-orbit", {
          y: 140,
          scale: 1.16,
          opacity: 0.1,
          scrollTrigger: {
            trigger: ".cosmos-hero",
            start: "top top",
            end: "bottom top",
            scrub: 1,
          },
        });
        root.current?.querySelectorAll(".gallery-section").forEach((section) =>
          gsap.from(section, {
            y: 65,
            opacity: 0.25,
            duration: 1,
            scrollTrigger: { trigger: section, start: "top 88%", once: true },
          }),
        );
        gsap.from(".film-frame", {
          scale: 0.88,
          scrollTrigger: {
            trigger: ".film-section",
            start: "top bottom",
            end: "top 20%",
            scrub: 1,
          },
        });
      },
      root,
    );
    return () => media.revert();
  }, []);
  async function toggleVideo() {
    if (!video.current) return;
    if (playing) video.current.pause();
    else
      try {
        await video.current.play();
      } catch {
        setVideoError(true);
      }
  }
  return (
    <div ref={root} className="cosmos-landing">
      <section className="cosmos-hero">
        <div className="hero-orbit">
          <BrandOrbit />
        </div>
        <div className="hero-copy">
          <p className="hero-eyebrow">ARIESHUB · AI 实践社区</p>
          <h1>
            探索 AI，
            <br />
            与同路人一起。
          </h1>
          <p className="hero-subtitle">分享实践，交流想法。</p>
          <Link
            className="cosmos-cta"
            href={hasSession ? "/home" : "/register"}
          >
            {hasSession ? "进入社区" : "加入社区"}
            <ArrowUpRight size={17} />
          </Link>
        </div>
        <a className="scroll-invitation" href="#film">
          向下，发现更多<span>↓</span>
        </a>
      </section>
      <section id="film" className="film-section gallery-section">
        <p className="section-kicker">
          <span />
          看见想法的可能
          <span />
        </p>
        <div className="film-frame">
          <video
            ref={video}
            src="/media/ai-film.mp4"
            poster="/media/ai-film-poster.jpg"
            playsInline
            muted={muted}
            loop
            preload="none"
            onPlay={() => setPlaying(true)}
            onPause={() => setPlaying(false)}
            onError={() => setVideoError(true)}
          />
          <div className={`film-overlay ${playing ? "is-playing" : ""}`}>
            <p>IMAGINATION, IN MOTION</p>
            <h2>想象，从这里发生。</h2>
            <Button
              className="film-play"
              aria-label={playing ? "暂停影片" : "播放影片"}
              onClick={toggleVideo}
            >
              {playing ? <Pause /> : <Play fill="currentColor" />}
            </Button>
          </div>
          <div className="film-controls">
            <span>AI 影像 · 灵感放映室</span>
            <Button
              variant="ghost"
              onClick={() => setMuted(!muted)}
              aria-label={muted ? "开启声音" : "静音"}
            >
              {muted ? <VolumeX /> : <Volume2 />}
            </Button>
          </div>
        </div>
        {videoError && <p role="status">影片暂时无法播放，请稍后重试。</p>}
        <p className="asset-credit">
          公开创作片段 ·{" "}
          <a
            href="https://pixabay.com/videos/ai-generated-astronaut-universe-257794/"
            target="_blank"
            rel="noreferrer"
          >
            查看作品来源 ↗
          </a>
        </p>
      </section>
      <section id="explore" className="gallery-section explore-section">
        <p className="section-kicker">从工具开始，走向自己的作品</p>
        <h2>从好奇，到亲手实践。</h2>
        <div className="topic-tabs" role="group" aria-label="实践方向">
          {topics.map((t, i) => (
            <Button
              variant="ghost"
              key={t}
              aria-pressed={active === i}
              onClick={() => setActive(i)}
            >
              {t}
            </Button>
          ))}
        </div>
        <div className={`practice-stage stage-${active}`}>
          <div className="stage-main" key={active}>
            <Image
              unoptimized
              src={conceptCases[active].image}
              alt={conceptCases[active].title}
              width={900}
              height={650}
            />
            <div>
              <span>0{active + 1} / PRACTICE</span>
              <h3>{conceptCases[active].shortTitle}</h3>
              <Link href={`/cases/${conceptCases[active].slug}`}>
                探索这篇内容 <ArrowUpRight size={18} />
              </Link>
            </div>
          </div>
          <div className="stage-side">
            <Image
              unoptimized
              src={conceptCases[(active + 1) % 4].image}
              alt="更多实践内容预览"
              width={400}
              height={500}
            />
          </div>
        </div>
        <div className="carousel-controls">
          <Button
            variant="ghost"
            aria-label="上一个实践方向"
            onClick={() => setActive((active + 3) % 4)}
          >
            <ArrowLeft />
          </Button>
          <span>
            0{active + 1} <i>/ 04</i>
          </span>
          <Button
            variant="ghost"
            aria-label="下一个实践方向"
            onClick={() => setActive((active + 1) % 4)}
          >
            <ArrowRight />
          </Button>
        </div>
      </section>
      <section id="stories" className="gallery-section stories-section">
        <p className="section-kicker">来自主理人的深度分享</p>
        <h2>
          不止看结果，
          <br />
          也聊怎么做到。
        </h2>
        <div className="story-grid">
          {conceptCases.slice(0, 2).map((item, i) => (
            <Link
              className={`story-card story-${i}`}
              key={item.slug}
              href={`/cases/${item.slug}`}
            >
              <Image src={item.image} alt="" width={900} height={700} />
              <div>
                <span>实 践 拆 解 · 0{i + 1}</span>
                <h3>{item.shortTitle}</h3>
                <ArrowUpRight />
              </div>
            </Link>
          ))}
        </div>
        <p className="quiet-note">内容预览 · 部分深度内容需付费解锁</p>
      </section>
      <section id="community" className="gallery-section community-section">
        <p className="section-kicker">COMMUNITY, NOT ALONE</p>
        <h2>
          一个人探索，
          <br />
          一群人交流。
        </h2>
        <div className="conversation-art">
          <div className="conversation-orbit" />
          <div className="conversation-bubble">
            <span className="avatar-blue">想</span>工作流卡在这一步，怎么调整？
          </div>
          <div className="conversation-bubble">
            <span className="avatar-green">试</span>可以拆开输入，再逐步验证。
          </div>
          <div className="conversation-bubble">
            <span className="avatar-orange">做</span>我把尝试过程整理出来了。
            <Asterisk size={18} />
          </div>
        </div>
        <p className="quiet-note">交流场景示意</p>
        <Link className="cosmos-text-link" href="/community">
          看看社区讨论 <ArrowUpRight size={16} />
        </Link>
      </section>
      <section id="about" className="gallery-section founder-section">
        <p className="section-kicker">来自主理人</p>
        <h2>
          分享走过的路，
          <br />
          也探索新的可能。
        </h2>
        <div className="founder-grid">
          <Image
            unoptimized
            src="/concepts/knowledge-color.webp"
            alt="创作空间概念图"
            width={750}
            height={650}
          />
          <div>
            <Asterisk size={30} />
            <p>
              我会在这里持续分享 AI 实践，
              <br />
              也期待与你交流新的想法。
            </p>
            <span>深度文章、实践拆解，以及过程中的发现。</span>
            <Link className="cosmos-text-link" href="/cases">
              浏览内容 <ArrowUpRight size={16} />
            </Link>
          </div>
        </div>
      </section>
      <footer className="cosmos-footer">
        <BrandOrbit compact />
        <div className="footer-copy">
          <p>下一次实践，一起开始。</p>
          <Link
            className="cosmos-cta"
            href={hasSession ? "/home" : "/register"}
          >
            {hasSession ? "进入 AriesHub" : "加入 AriesHub"}
            <ArrowUpRight />
          </Link>
          <small>
            主理人分享深度内容，成员交流实践。
            <br />
            部分内容需付费解锁。
          </small>
        </div>
        <div className="footer-links">
          <a href="#about">关于社区</a>
          <Link href="/login">登录</Link>
          <a href="#main">回到顶部 ↑</a>
        </div>
        <div className="footer-wordmark" aria-hidden="true">
          AriesHub
        </div>
      </footer>
    </div>
  );
}
