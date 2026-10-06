"use client";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import gsap from "gsap";
import { ArrowLeft, Asterisk, Check, Eye, EyeOff, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authRequest, type ApiError, type CurrentUser } from "@/lib/auth";
import { brands } from "@/lib/brand-icons";
const headings = [
  "输入你的邮箱",
  "查看你的收件箱",
  "设置你的密码",
  "怎么称呼你？",
  "你想探索什么？",
];
const subtitles = [
  "从这里，开始新的探索。",
  "输入邮件中的 6 位验证码。",
  "至少 8 位，包含字母和数字。",
  "这个名字会展示在你的个人主页。",
  "选几个感兴趣的方向，也可以稍后再说。",
];
const interests = [
  "AI 编程",
  "自动化",
  "产品设计",
  "内容创作",
  "独立开发",
  "影像创作",
];
export function AuthExperience({
  mode,
  initialEmail = "",
}: {
  mode: "login" | "register";
  initialEmail?: string;
}) {
  const register = mode === "register";
  const router = useRouter();
  const root = useRef<HTMLDivElement>(null);
  const form = useRef<HTMLFormElement>(null);
  const timeline = useRef<gsap.core.Timeline | null>(null);
  const [step, setStep] = useState(0);
  const [email, setEmail] = useState(initialEmail);
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [nickname, setNickname] = useState("");
  const [selected, setSelected] = useState<string[]>([]);
  const [show, setShow] = useState(false);
  const [pending, setPending] = useState(false);
  const [transitioning, setTransitioning] = useState(false);
  const [error, setError] = useState("");
  const [requestId, setRequestId] = useState("");
  const [cooldown, setCooldown] = useState(0);
  const [demo, setDemo] = useState(false);
  const [sent, setSent] = useState(false);
  const [retryAt, setRetryAt] = useState(0);
  const [retrySeconds, setRetrySeconds] = useState(0);
  useEffect(() => {
    if (!retryAt) return;
    const tick = () => {
      const remaining = Math.max(0, Math.ceil((retryAt - Date.now()) / 1000));
      setRetrySeconds(remaining);
      if (!remaining) {
        setRetryAt(0);
        setError("");
        setRequestId("");
      }
    };
    tick();
    const timer = setInterval(tick, 250);
    return () => clearInterval(timer);
  }, [retryAt]);
  function showError(error: ApiError) {
    setError(error.message);
    setRequestId(error.requestId ?? "");
    if (error.retryAfterSeconds && error.retryAfterSeconds > 0) {
      setRetrySeconds(error.retryAfterSeconds);
      setRetryAt(Date.now() + error.retryAfterSeconds * 1000);
    }
  }
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown(cooldown - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);
  useEffect(
    () => () => {
      timeline.current?.kill();
    },
    [],
  );
  useEffect(() => {
    const ctx = gsap.context(() => {
      if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        gsap.fromTo(
          ".auth-art-card",
          { scale: 0.65, opacity: 0, x: 0, y: 0 },
          {
            scale: 1,
            opacity: 1,
            x: 0,
            y: 0,
            stagger: 0.055,
            duration: 0.65,
            ease: "power3.out",
          },
        );
        gsap.fromTo(
          ".auth-step-content",
          { opacity: 0, y: 15 },
          { opacity: 1, y: 0, duration: 0.4 },
        );
      }
    }, root);
    if (step > 0)
      form.current
        ?.querySelector<HTMLInputElement>("input")
        ?.focus({ preventScroll: true });
    return () => ctx.revert();
  }, [step]);
  function move(next: number) {
    if (transitioning || pending) return;
    setError("");
    setRequestId("");
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      setStep(next);
      return;
    }
    setTransitioning(true);
    const cards = root.current?.querySelectorAll(".auth-art-card");
    timeline.current = gsap
      .timeline({
        onComplete: () => {
          setStep(next);
          setTransitioning(false);
        },
      })
      .to(
        cards ?? [],
        {
          scale: 0.45,
          opacity: 0,
          x: (_i, card: HTMLElement) =>
            (card.parentElement?.clientWidth ?? 0) / 2 -
            card.offsetLeft -
            card.offsetWidth / 2,
          y: (_i, card: HTMLElement) =>
            (card.parentElement?.clientHeight ?? 0) / 2 -
            card.offsetTop -
            card.offsetHeight / 2,
          stagger: 0.035,
          duration: 0.3,
          ease: "power2.in",
        },
        0,
      )
      .to(
        root.current?.querySelector(".auth-step-content") ?? [],
        { opacity: 0, y: -12, duration: 0.2 },
        0,
      );
  }
  function redirectExistingAccount(code: string) {
    if (!register || code !== "EMAIL_ALREADY_REGISTERED") return false;
    router.replace(`/login?${new URLSearchParams({ email: email.trim() })}`);
    return true;
  }
  async function sendCode() {
    if (pending || retrySeconds > 0) return false;
    if (cooldown > 0) {
      setError(`请等待 ${cooldown} 秒后再发送验证码`);
      return false;
    }
    setError("");
    setRequestId("");
    setPending(true);
    if (demo) {
      setCooldown(30);
      setSent(true);
      setPending(false);
      return true;
    }
    const result = await authRequest<{ resendAfterSeconds: number }>(
      "/email-codes",
      {
        method: "POST",
        body: JSON.stringify({ email: email.trim(), purpose: "REGISTER" }),
      },
    );
    setPending(false);
    if (!result.ok) {
      if (redirectExistingAccount(result.error.code)) return false;
      showError(result.error);
      return false;
    }
    setCooldown(result.data.resendAfterSeconds);
    setSent(true);
    return true;
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (pending || transitioning || retrySeconds > 0) return;
    setError("");
    setRequestId("");
    if (
      (!register || step === 0) &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())
    ) {
      setError("请输入有效的邮箱地址");
      return;
    }
    if (!register && !password) {
      setError("请输入密码");
      return;
    }
    if (register && step < 4) {
      if (step === 0) {
        if (!sent && !(await sendCode())) return;
        move(1);
        return;
      }
      if (step === 1 && !/^\d{6}$/.test(code)) {
        setError("请输入 6 位数字验证码");
        return;
      }
      if (step === 1 && demo && code !== "123456") {
        setError("演示验证码为 123456");
        return;
      }
      if (step === 2 && !/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(password)) {
        setError("密码需为 8–72 位，包含字母和数字");
        return;
      }
      if (step === 3 && nickname.trim().length < 2) {
        setError("昵称至少需要 2 个字符");
        return;
      }
      move(step + 1);
      return;
    }
    setPending(true);
    if (demo) {
      sessionStorage.setItem(
        "arieshub-demo-interests",
        JSON.stringify(selected),
      );
      router.push("/discover?preview=1");
      return;
    }
    const result = await authRequest<CurrentUser>(
      register ? "/register" : "/login",
      {
        method: "POST",
        body: JSON.stringify({
          email: email.trim(),
          password,
          ...(register ? { nickname: nickname.trim(), code } : {}),
        }),
      },
    );
    setPending(false);
    if (!result.ok) {
      if (redirectExistingAccount(result.error.code)) return false;
      showError(result.error);
      return;
    }
    window.dispatchEvent(
      new CustomEvent("arieshub:auth", { detail: result.data }),
    );
    router.push("/home");
    router.refresh();
  }
  const emailField = (
    <div className="studio-field">
      <Label htmlFor="studio-email" className="sr-only">
        邮箱
      </Label>
      <Input
        id="studio-email"
        type="email"
        name="email"
        value={email}
        onChange={(e) => {
          setEmail(e.target.value);
          setSent(false);
        }}
        autoComplete="email"
        placeholder="邮箱地址"
        required
        maxLength={254}
      />
    </div>
  );
  const passwordField = (
    <div className="studio-field password-field">
      <Label className="sr-only" htmlFor="studio-password">
        密码
      </Label>
      <Input
        id="studio-password"
        name="password"
        type={show ? "text" : "password"}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        autoComplete={register ? "new-password" : "current-password"}
        placeholder={register ? "至少 8 位，包含字母和数字" : "密码"}
        required
        minLength={register ? 8 : undefined}
        maxLength={72}
      />
      <Button
        type="button"
        variant="ghost"
        aria-label={show ? "隐藏密码" : "显示密码"}
        onClick={() => setShow(!show)}
      >
        {show ? <Eye size={18} /> : <EyeOff size={18} />}
      </Button>
    </div>
  );
  return (
    <div className="studio-auth" ref={root}>
      <section className="auth-left">
        <div className="auth-top">
          <Button
            variant="ghost"
            aria-label={register && step > 0 ? "上一步" : "返回首页"}
            disabled={pending || transitioning || retrySeconds > 0}
            onClick={() =>
              register && step > 0 ? move(step - 1) : router.push("/")
            }
          >
            <ArrowLeft size={20} />
          </Button>
          <Link href="/" aria-label="AriesHub 首页">
            <Asterisk size={32} />
          </Link>
          <span />
        </div>
        {register && (
          <div
            className="auth-progress"
            aria-label={`第 ${step + 1} 步，共 5 步`}
          >
            {headings.map((h, i) => (
              <span key={h} className={i <= step ? "done" : ""} />
            ))}
          </div>
        )}
        <form
          ref={form}
          className="studio-auth-form"
          noValidate
          onSubmit={submit}
        >
          <div className="auth-step-content">
            <header>
              <h1>{register ? headings[step] : "欢迎回来"}</h1>
              <p>{register ? subtitles[step] : "继续探索，也继续创造。"}</p>
            </header>
            {(!register || step === 0) && emailField}
            {(!register || step === 2) && passwordField}
            {register && step === 1 && (
              <>
                <div className="studio-field">
                  <Label className="sr-only" htmlFor="studio-code">
                    邮箱验证码
                  </Label>
                  <Input
                    id="studio-code"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    placeholder="6 位验证码"
                    required
                    pattern="[0-9]{6}"
                    maxLength={6}
                  />
                </div>
                <p className="code-hint">
                  {demo ? "演示验证码：123456" : `已发送至 ${email}`}
                </p>
                <Button
                  type="button"
                  variant="ghost"
                  disabled={pending || cooldown > 0 || retrySeconds > 0}
                  onClick={() => void sendCode()}
                >
                  {cooldown > 0 ? `${cooldown}s 后重发` : "重新发送验证码"}
                </Button>
              </>
            )}
            {register && step === 3 && (
              <div className="studio-field">
                <Label className="sr-only" htmlFor="studio-nickname">
                  怎么称呼你
                </Label>
                <Input
                  id="studio-nickname"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="你的昵称"
                  autoComplete="nickname"
                  required
                  minLength={2}
                  maxLength={30}
                />
              </div>
            )}
            {register && step === 4 && (
              <div className="interest-grid">
                {interests.map((item) => (
                  <Button
                    key={item}
                    type="button"
                    variant="outline"
                    aria-pressed={selected.includes(item)}
                    onClick={() =>
                      setSelected(
                        selected.includes(item)
                          ? selected.filter((x) => x !== item)
                          : [...selected, item],
                      )
                    }
                  >
                    {item}
                    {selected.includes(item) ? (
                      <Check size={16} />
                    ) : (
                      <Plus size={16} />
                    )}
                  </Button>
                ))}
              </div>
            )}
            {error && (
              <p role="alert" className="form-error">
                {retrySeconds > 0
                  ? `操作过于频繁，请在 ${Math.floor(retrySeconds / 60)} 分 ${retrySeconds % 60} 秒后重试`
                  : error}
                {requestId && <small>请求编号：{requestId}</small>}
                {register && step === 4 && (
                  <Button variant="link" type="button" onClick={() => move(1)}>
                    返回修改验证码
                  </Button>
                )}
              </p>
            )}
            <Button
              className="studio-submit"
              type="submit"
              disabled={pending || transitioning || retrySeconds > 0}
            >
              {retrySeconds > 0
                ? `${Math.floor(retrySeconds / 60)}:${String(retrySeconds % 60).padStart(2, "0")} 后可重试`
                : pending
                  ? "请稍候…"
                  : register
                    ? step === 0
                      ? "获取验证码"
                      : step === 4
                        ? "加入 AriesHub"
                        : "继续"
                    : "登录 AriesHub"}
            </Button>
            <p className="studio-switch">
              {register ? "已经有账号？" : "还没有账号？"}{" "}
              <Link href={register ? "/login" : "/register"}>
                {register ? "登录" : "注册"}
              </Link>
            </p>
          </div>
          <div className="demo-mode">
            {demo ? (
              <p role="status">演示模式 · 不发送邮件，不创建真实账号</p>
            ) : (
              <Button
                type="button"
                variant="link"
                onClick={() => {
                  setDemo(true);
                  setError("");
                }}
              >
                仅体验页面流程
              </Button>
            )}
          </div>
        </form>
        <Link className="auth-wordmark" href="/">
          AriesHub
        </Link>
      </section>
      <aside className={`auth-right composition-${step}`} aria-label="灵感画廊">
        <div className="auth-art-group" key={step}>
          {["architecture", "presentation", "automation", "knowledge"].map(
            (name, i) => (
              <div className={`auth-art-card art-${i}`} key={name}>
                <Image
                  unoptimized
                  src={`/concepts/${["architecture", "presentation", "automation", "knowledge"][(i + step) % 4]}-color.webp`}
                  alt=""
                  fill
                  sizes="(max-width: 800px) 0px, 30vw"
                  priority={i === 0}
                />
              </div>
            ),
          )}
          <div className="auth-product-badge">
            <Image
              unoptimized
              src={`/brands/${brands[(step * 3) % brands.length][0]}.svg`}
              alt={brands[(step * 3) % brands.length][1]}
              width={36}
              height={36}
            />
          </div>
        </div>
        <div className="auth-art-caption">
          <span>IDEAS BECOME PRACTICE.</span>
          <p>
            {
              [
                "从一个好奇，开始新的可能。",
                "好的想法，值得一起探索。",
                "把灵感，变成下一次实践。",
                "每个人，都有自己的视角。",
                "找到你的兴趣，也找到同路人。",
              ][step]
            }
          </p>
        </div>
      </aside>
    </div>
  );
}
