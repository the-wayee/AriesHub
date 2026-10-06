"use client";
import {
  AVATAR_MAX_BYTES,
  IMAGE_FILE_ACCEPT,
  IMAGE_MIME_TYPES,
} from "@/lib/file-types";

import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import {
  Camera,
  Check,
  ArrowUpRight,
  Mail,
  LogOut,
  LoaderCircle,
} from "lucide-react";
import { useGSAP } from "@gsap/react";
import gsap from "gsap";
import type { CurrentUser } from "@/lib/auth";
import { userRequest } from "@/lib/user";
import { uploadAvatar } from "@/lib/user";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useAuthSession } from "./auth-session";
import { UserAvatar } from "./user-avatar";
import { useLogout } from "./use-logout";

gsap.registerPlugin(useGSAP);

export function AccountPanel() {
  // SessionGate 已验证当前用户；资料页和导航共用会话，避免挂载后再次读取 /me。
  // 登录校验失败的提示与重试由守卫统一处理。
  const { user } = useAuthSession();
  if (user === undefined)
    return (
      <div className="profile-skeleton" role="status" aria-label="正在读取账号">
        <div />
        <div />
        <span className="sr-only">正在读取账号…</span>
      </div>
    );
  if (!user)
    return (
      <section className="profile-empty">
        <span className="profile-empty-mark">
          <Camera />
        </span>
        <h2>你的社区名片，从这里开始</h2>
        <p>登录后设置头像、昵称和个性签名。</p>
        <Link className="profile-primary-link" href="/login">
          登录账号 <ArrowUpRight />
        </Link>
        <Link href="/discover">先去探索内容</Link>
      </section>
    );
  return <ProfileEditor key={user.id} user={user} />;
}

function ProfileEditor({ user }: { user: CurrentUser }) {
  const { avatarUrl } = useAuthSession();
  const { logout, busy: loggingOut, error: logoutError } = useLogout();
  const [nickname, setNickname] = useState(user.nickname);
  const [bio, setBio] = useState(user.bio ?? "");
  const [avatarId, setAvatarId] = useState(user.avatarFileId ?? null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const root = useRef<HTMLDivElement>(null);
  const input = useRef<HTMLInputElement>(null);
  const dirty =
    nickname.trim() !== user.nickname ||
    bio.trim() !== (user.bio ?? "") ||
    avatarId !== (user.avatarFileId ?? null) ||
    file !== null;
  useEffect(
    () => () => {
      if (preview) URL.revokeObjectURL(preview);
    },
    [preview],
  );
  useGSAP(
    () => {
      const media = gsap.matchMedia();
      media.add("(prefers-reduced-motion: no-preference)", () => {
        gsap.fromTo(
          ".profile-reveal",
          { opacity: 0, y: 14 },
          {
            opacity: 1,
            y: 0,
            duration: 0.45,
            stagger: 0.07,
            ease: "power2.out",
            clearProps: "all",
          },
        );
      });
      return () => media.revert();
    },
    { scope: root },
  );
  function chooseAvatar(selected?: File) {
    if (!selected) return;
    setError("");
    setMessage("");
    if (!IMAGE_MIME_TYPES.includes(selected.type)) {
      setError("请选择 PNG、JPG 或 WebP 图片");
      return;
    }
    if (selected.size === 0 || selected.size > AVATAR_MAX_BYTES) {
      setError("头像需为不超过 5 MB 的图片");
      return;
    }
    setFile(selected);
    setPreview(URL.createObjectURL(selected));
  }
  function reset() {
    setNickname(user.nickname);
    setBio(user.bio ?? "");
    setAvatarId(user.avatarFileId ?? null);
    setFile(null);
    setPreview(null);
    setError("");
    setMessage("");
  }
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    if (nickname.trim().length < 2 || nickname.trim().length > 30) {
      setError("昵称需为 2–30 字");
      return;
    }
    setBusy(true);
    setError("");
    setMessage("");
    let nextAvatar = avatarId;
    if (file) {
      const upload = await uploadAvatar(file);
      if (!upload.ok) {
        setError(upload.error.msg);
        setBusy(false);
        return;
      }
      nextAvatar = upload.id;
      setAvatarId(nextAvatar);
      setFile(null);
    }
    const result = await userRequest<CurrentUser>("/me/profile", {
      method: "PUT",
      body: JSON.stringify({
        nickname: nickname.trim(),
        bio: bio.trim(),
        avatarFileId: nextAvatar,
      }),
    });
    setBusy(false);
    if (!result.ok) {
      setError(result.error.msg);
      return;
    }
    setNickname(result.data.nickname);
    setBio(result.data.bio ?? "");
    setAvatarId(result.data.avatarFileId ?? null);
    setPreview(null);
    window.dispatchEvent(
      new CustomEvent("arieshub:auth", { detail: result.data }),
    );
    setMessage("资料已保存");
  }
  const image = preview ?? (avatarId === user.avatarFileId ? avatarUrl : null);
  const joined = new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
  }).format(new Date(user.createdAt));
  return (
    <div className="profile-layout" ref={root}>
      <aside className="profile-card profile-reveal" aria-label="个人名片预览">
        <div className="profile-card-cover">
          <span>AriesHub / community</span>
          <i aria-hidden="true" />
        </div>
        <div className="profile-card-body">
          <UserAvatar
            name={nickname || user.nickname}
            url={image}
            className="profile-card-avatar"
          />
          <p className="profile-role">
            {user.role === "ADMIN" ? "社区主理人" : "社区成员"}
          </p>
          <h2>{nickname.trim() || user.nickname}</h2>
          <p className="profile-bio">
            {bio.trim() || "保持好奇，从一次实践开始。"}
          </p>
          <div className="profile-card-foot">
            <span>{joined}加入</span>
            <span>
              一起探索 <ArrowUpRight />
            </span>
          </div>
        </div>
        <p className="profile-preview-note">你的名片预览</p>
      </aside>
      <div className="profile-settings">
        <form className="profile-editor profile-reveal" onSubmit={save}>
          <div className="profile-section-heading">
            <div>
              <p>个人资料</p>
              <h2>一点关于你的介绍</h2>
            </div>
            <span>01</span>
          </div>
          <fieldset disabled={busy}>
            <legend className="sr-only">编辑个人资料</legend>
            <div className="profile-avatar-row">
              <UserAvatar
                name={nickname || user.nickname}
                url={image}
                className="profile-editor-avatar"
              />
              <div>
                <strong>社区头像</strong>
                <p>PNG、JPG 或 WebP，最大 5 MB</p>
                <div className="profile-avatar-actions">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => input.current?.click()}
                  >
                    <Camera />
                    {image || avatarId ? "更换头像" : "上传头像"}
                  </Button>
                  {(avatarId || file) && (
                    <Button
                      variant="ghost"
                      type="button"
                      onClick={() => {
                        setFile(null);
                        setPreview(null);
                        setAvatarId(null);
                        setError("");
                        setMessage("");
                      }}
                    >
                      移除头像
                    </Button>
                  )}
                </div>
                <input
                  ref={input}
                  className="sr-only"
                  type="file"
                  accept={IMAGE_FILE_ACCEPT}
                  aria-label="选择头像图片"
                  tabIndex={-1}
                  onChange={(event) => {
                    chooseAvatar(event.target.files?.[0]);
                    event.target.value = "";
                  }}
                />
              </div>
            </div>
            <div className="profile-field">
              <label htmlFor="profile-name">怎么称呼你</label>
              <Input
                id="profile-name"
                autoComplete="nickname"
                required
                minLength={2}
                maxLength={30}
                value={nickname}
                onChange={(event) => {
                  setNickname(event.target.value);
                  setMessage("");
                }}
              />
              <small>在社区交流时使用，2–30 字。</small>
            </div>
            <div className="profile-field">
              <label htmlFor="profile-bio">个性签名</label>
              <Textarea
                id="profile-bio"
                value={bio}
                maxLength={160}
                rows={3}
                placeholder="最近在探索什么？用一句话介绍自己。"
                onChange={(event) => {
                  setBio(event.target.value);
                  setMessage("");
                }}
                aria-describedby="bio-count"
              />
              <div className="profile-field-note">
                <small>可以是你的兴趣、方向，或一句喜欢的话。</small>
                <small id="bio-count">{bio.length} / 160</small>
              </div>
            </div>
          </fieldset>
          {error && (
            <p className="profile-feedback profile-feedback-error" role="alert">
              {error}
            </p>
          )}
          <div className="profile-save-row">
            <p role="status">
              {message ? (
                <>
                  <Check />
                  {message}
                </>
              ) : dirty ? (
                "有尚未保存的修改"
              ) : (
                "让每次相遇，多一点了解。"
              )}
            </p>
            <div>
              <Button
                type="button"
                variant="ghost"
                onClick={reset}
                disabled={!dirty || busy}
              >
                取消修改
              </Button>
              <Button
                type="submit"
                className="profile-save"
                disabled={!dirty || busy}
              >
                {busy ? (
                  <>
                    <LoaderCircle className="profile-spinner" />
                    正在保存…
                  </>
                ) : (
                  "保存资料"
                )}
              </Button>
            </div>
          </div>
        </form>
        <section
          className="profile-security profile-reveal"
          aria-labelledby="account-details"
        >
          <div className="profile-section-heading">
            <div>
              <p>账号信息</p>
              <h2 id="account-details">安心地留在这里</h2>
            </div>
            <span>02</span>
          </div>
          <div className="profile-email">
            <Mail />
            <div>
              <strong>{user.email}</strong>
              <small>登录邮箱</small>
            </div>
            <span className="profile-verified">
              {user.emailVerified ? (
                <>
                  <Check />
                  已验证
                </>
              ) : (
                "待验证"
              )}
            </span>
          </div>
          <div className="profile-signout">
            <p>使用公共设备时，记得在离开前退出。</p>
            <Button
              type="button"
              variant="ghost"
              onClick={() => void logout()}
              disabled={loggingOut || busy}
            >
              <LogOut />
              {loggingOut ? "正在退出…" : "退出登录"}
            </Button>
          </div>
          {logoutError && (
            <p className="profile-feedback-error" role="alert">
              {logoutError}
            </p>
          )}
        </section>
      </div>
    </div>
  );
}
