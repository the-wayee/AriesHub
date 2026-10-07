"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import { Popover } from "@base-ui/react/popover";
import { QRCodeSVG } from "qrcode.react";
import { Check, Copy, Share2, Send } from "lucide-react";
import { useAuthSession } from "./auth-session";
import { apiRequest } from "@/lib/api";
import type { components } from "@/lib/api-schema";
import { recordPublicationEvent } from "@/lib/publication-events";
import { usePublicationMetrics } from "./publication-metrics";

const QQ_SHARE_ENDPOINT = "https://connect.qq.com/widget/shareqq/index.html";

/** 点击分享计数，悬浮仅预览；计数由服务端按账号与文章唯一授权去重。 */
export function PublicationShare({ id, title }: { id: string; title: string }) {
  const metrics = usePublicationMetrics(id);
  const [linkRevision, setLinkRevision] = useState(0);
  const { user } = useAuthSession();
  const userId = user?.id;
  const key = `${id}:${userId ?? "anon"}:${linkRevision}`;
  const [snapshot, setSnapshot] = useState<{
    key: string;
    data?: components["schemas"]["PublicationShareLink"];
    error?: string;
  }>();
  useEffect(() => {
    if (!userId) return;
    let active = true;
    // 严格模式重放及重复加载仍复用同一条后端分享授权。
    void apiRequest<components["schemas"]["PublicationShareLink"]>(
      `/api/v1/publications/${id}/share-link`,
      { method: "POST" },
    ).then((result) => {
      if (active)
        setSnapshot(
          result.ok
            ? { key, data: result.data }
            : { key, error: result.error.msg },
        );
    });
    return () => {
      active = false;
    };
  }, [id, key, userId]);
  const link = snapshot?.key === key ? snapshot.data : undefined;
  const linkError = snapshot?.key === key ? snapshot.error : undefined;
  const [open, setOpen] = useState(false);
  const confirmed = useRef<string | undefined>(undefined);
  const tracking = useRef(false);
  const copying = useRef(false);
  const [trackingError, setTrackingError] = useState("");
  const [pending, setPending] = useState(false);
  const [copied, setCopied] = useState(false);
  const [copyFailed, setCopyFailed] = useState(false);
  const [channelHint, setChannelHint] = useState("");

  async function countClick() {
    if (!link || confirmed.current === link.token || tracking.current) return;
    tracking.current = true;
    try {
      const result = await recordPublicationEvent(id, "share", link.token);
      if (result.ok) confirmed.current = link.token;
      setTrackingError(result.ok ? "" : "分享统计暂未同步");
    } finally {
      tracking.current = false;
    }
  }
  async function copyLink() {
    if (copying.current || !link) return;
    copying.current = true;
    setPending(true);
    setCopied(false);
    setCopyFailed(false);
    try {
      // 复制本身不再计数；也允许用户只悬浮预览后复制，仍按一次明确分享动作确认。
      void countClick();
      await navigator.clipboard.writeText(link.url);
      setCopied(true);
    } catch {
      setCopyFailed(true);
    } finally {
      copying.current = false;
      setPending(false);
    }
  }
  async function nativeShare() {
    if (!link) return;
    void countClick();
    if (!navigator.share) {
      await copyLink();
      return;
    }
    try {
      await navigator.share({ title, url: link.url });
    } catch (error) {
      if (!(error instanceof DOMException && error.name === "AbortError"))
        setChannelHint("暂时无法打开系统分享，可复制链接发送。");
    }
  }
  const qqUrl = link
    ? `${QQ_SHARE_ENDPOINT}?${new URLSearchParams({ url: link.url, title, summary: link.summary ?? "", site: "AriesHub" })}`
    : "";
  const local = link
    ? ["localhost", "127.0.0.1"].includes(new URL(link.url).hostname)
    : false;
  return (
    <div className="publication-share">
      <Popover.Root
        open={open}
        onOpenChange={(next, details) =>
          setOpen(details.reason === "trigger-press" ? true : next)
        }
      >
        <Popover.Trigger
          className="publication-share-trigger"
          disabled={!link}
          aria-label="分享文章"
          openOnHover
          delay={180}
          closeDelay={220}
          onClick={() => void countClick()}
        >
          <Share2 size={18} />
          <span>分享</span>
          <span className="publication-action-number">
            {metrics?.shareCount?.toLocaleString() ?? "—"}
          </span>
        </Popover.Trigger>
        <Popover.Portal>
          <Popover.Positioner
            side="top"
            align="center"
            sideOffset={12}
            className="publication-share-positioner"
          >
            <Popover.Popup className="publication-share-popup publication-share-preview">
              <Popover.Title>把这篇文章分享给朋友</Popover.Title>
              {link && (
                <>
                  <div className="share-preview-grid">
                    <div className="share-preview-story">
                      {link.cover?.url && (
                        <Image
                          className="share-preview-cover"
                          src={link.cover.url}
                          alt={title}
                          width={260}
                          height={146}
                          unoptimized
                        />
                      )}
                      <strong>{title}</strong>
                      {link.summary && <p>{link.summary}</p>}
                    </div>
                    <div className="share-preview-code">
                      <QRCodeSVG
                        value={link.url}
                        size={120}
                        level="M"
                        marginSize={4}
                        title="文章分享二维码"
                      />
                      <span>手机扫码阅读</span>
                    </div>
                  </div>
                  <div className="share-preview-link">
                    <input
                      aria-label="文章分享链接"
                      readOnly
                      value={link.url}
                      onFocus={(event) => event.currentTarget.select()}
                    />
                    <button
                      type="button"
                      aria-label={copied ? "链接已复制" : "复制链接"}
                      disabled={pending}
                      onClick={() => void copyLink()}
                    >
                      {copied ? <Check size={16} /> : <Copy size={16} />}
                      <span role="status">{copied ? "已复制" : "复制"}</span>
                    </button>
                  </div>
                  <div className="share-preview-channels" aria-label="分享方式">
                    <button
                      type="button"
                      onClick={() => {
                        void countClick();
                        setChannelHint(
                          "微信扫一扫上方二维码，打开文章后可转发给好友。",
                        );
                      }}
                    >
                      <span data-brand="wechat">
                        <Image
                          src="/brands/wechat.svg"
                          width={21}
                          height={21}
                          alt=""
                        />
                      </span>
                      微信
                    </button>
                    <a
                      href={qqUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={() => void countClick()}
                    >
                      <span data-brand="qq">
                        <Image
                          src="/brands/qq.svg"
                          width={21}
                          height={21}
                          alt=""
                        />
                      </span>
                      QQ
                    </a>
                    <button
                      type="button"
                      aria-label="复制分享地址"
                      onClick={() => void copyLink()}
                    >
                      <span data-brand="link">
                        <Copy size={20} />
                      </span>
                      复制链接
                    </button>
                    <button type="button" onClick={() => void nativeShare()}>
                      <span data-brand="system">
                        <Send size={20} />
                      </span>
                      更多方式
                    </button>
                  </div>
                  {channelHint && (
                    <p className="share-channel-hint" role="status">
                      {channelHint}
                    </p>
                  )}
                  {local && (
                    <small className="share-local-note">
                      当前链接仅在这台电脑上可访问。
                    </small>
                  )}
                  {copyFailed && (
                    <small role="alert">
                      自动复制失败，请手动复制上方链接。
                    </small>
                  )}
                </>
              )}
            </Popover.Popup>
          </Popover.Positioner>
        </Popover.Portal>
      </Popover.Root>
      {linkError && (
        <p className="publication-share-error" role="alert">
          {linkError}
          <button
            type="button"
            onClick={() => setLinkRevision((value) => value + 1)}
          >
            重试
          </button>
        </p>
      )}
      {trackingError && (
        <p className="publication-share-error" role="status">
          {trackingError}
          <button type="button" onClick={() => void countClick()}>
            重试同步
          </button>
        </p>
      )}
    </div>
  );
}
