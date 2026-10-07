"use client";

import Link from "next/link";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";
gsap.registerPlugin(useGSAP);
import { useCallback, useEffect, useRef, useState } from "react";
import { Heart, MessageCircle, Send, Smile, Trash2 } from "lucide-react";
import { Popover } from "@base-ui/react/popover";
import { apiRequest } from "@/lib/api";
import type { components } from "@/lib/api-schema";
import { useAuthSession } from "./auth-session";
import { UserAvatar } from "./user-avatar";

type Comment = components["schemas"]["CommentView"];
type RootComment = components["schemas"]["RootCommentView"];
type CommentPage = components["schemas"]["CommentPage"];
type ReplyPage = components["schemas"]["ReplyPage"];
const COMMENT_LIMIT = 4000;
const PAGE_SIZE = 10;
const COMMENTS_API = "/api/v1/discussions/comments";
const COMMENT_EMOJIS = [
  ["😀", "开心"],
  ["😊", "微笑"],
  ["😂", "笑哭"],
  ["🥰", "喜欢"],
  ["🤔", "思考"],
  ["😎", "酷"],
  ["🥹", "感动"],
  ["😭", "哭泣"],
  ["😅", "汗颜"],
  ["🙌", "欢呼"],
  ["👍", "赞"],
  ["👏", "鼓掌"],
  ["🙏", "感谢"],
  ["💪", "加油"],
  ["❤️", "爱心"],
  ["🎉", "庆祝"],
  ["🔥", "火热"],
  ["✨", "闪亮"],
  ["💡", "灵感"],
  ["👀", "关注"],
  ["✅", "完成"],
  ["🚀", "出发"],
  ["☕", "咖啡"],
  ["🌱", "成长"],
] as const;
type Sort = "LATEST" | "HOT";

/** 使用现有讨论领域的稳定 slug 键，保持历史评论与管理后台使用同一线程。 */
export function PublicationComments({
  publicationId,
  targetKey,
  loginReturnTo,
}: {
  publicationId: string;
  targetKey: string;
  loginReturnTo?: string;
}) {
  const { user } = useAuthSession();
  const [data, setData] = useState<CommentPage>();
  const [sort, setSort] = useState<Sort>("LATEST");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const requestId = useRef(0);
  const loginUrl = `/login?next=${encodeURIComponent(loginReturnTo ?? `/publications/${publicationId}#comments`)}`;
  const load = useCallback(
    async (page = 1) => {
      const id = ++requestId.current;
      setLoading(true);
      setError("");
      const query = new URLSearchParams({
        targetType: "PUBLICATION",
        targetKey,
        sort,
        page: String(page),
        size: String(PAGE_SIZE),
      });
      const result = await apiRequest<CommentPage>(`${COMMENTS_API}?${query}`);
      // 排序、账号或文章切换时，旧请求不得覆盖新的评论列表。
      if (id !== requestId.current) return;
      setLoading(false);
      if (!result.ok) {
        setError(result.error.msg);
        return;
      }
      setData((previous) =>
        page === 1
          ? result.data
          : {
              ...result.data,
              items: [...(previous?.items ?? []), ...result.data.items],
            },
      );
    },
    [targetKey, sort],
  );
  useEffect(() => {
    let active = true;
    const requests = requestId;
    void Promise.resolve().then(() => {
      if (!active) return;
      setData(undefined);
      void load();
    });
    return () => {
      active = false;
      requests.current++;
    };
  }, [load, user?.id]);
  async function refresh() {
    setRevision((value) => value + 1);
    await load();
  }
  return (
    <section
      id="comments"
      className="publication-comments"
      aria-label="文章评论"
    >
      <header className="publication-comments-heading">
        <h2>
          讨论 <span>{data?.total ?? "—"}</span>
        </h2>
        <div aria-label="评论排序">
          {(
            [
              ["LATEST", "最新"],
              ["HOT", "最热"],
            ] as const
          ).map(([value, label]) => (
            <button
              type="button"
              key={value}
              aria-pressed={sort === value}
              onClick={() => setSort(value)}
            >
              {label}
            </button>
          ))}
        </div>
      </header>
      {user ? (
        <CommentComposer targetKey={targetKey} onSent={refresh} />
      ) : (
        <div className="comment-login">
          <MessageCircle size={18} />
          <span>登录后，分享你的想法</span>
          <Link href={loginUrl}>登录参与</Link>
        </div>
      )}
      {error && (
        <p role="alert" className="comment-error">
          {error}{" "}
          <button type="button" onClick={() => void load()}>
            重试
          </button>
        </p>
      )}
      {!data && loading && (
        <div className="comment-loading" role="status">
          正在加载评论…
        </div>
      )}
      {data?.total === 0 && (
        <p className="comment-empty">还没有评论，来聊聊你的看法。</p>
      )}
      <div className="comment-threads">
        {data?.items.map((root) => (
          <CommentThread
            key={`${root.comment.id}:${revision}`}
            root={root}
            targetKey={targetKey}
            loginUrl={loginUrl}
            onChanged={refresh}
          />
        ))}
      </div>
      {data && data.page < data.totalPages && (
        <button
          className="comment-load-more"
          type="button"
          disabled={loading}
          onClick={() => void load(data.page + 1)}
        >
          {loading ? "正在加载…" : "查看更多评论"}
        </button>
      )}
    </section>
  );
}

function CommentComposer({
  targetKey,
  parent,
  onSent,
  onCancel,
}: {
  targetKey: string;
  parent?: Comment;
  onSent: () => Promise<void>;
  onCancel?: () => void;
}) {
  const [body, setBody] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const submitting = useRef(false);
  const input = useRef<HTMLTextAreaElement>(null);
  const selection = useRef({ start: 0, end: 0 });
  const [emojiOpen, setEmojiOpen] = useState(false);
  function insertEmoji(emoji: string) {
    const { start, end } = selection.current;
    const next = body.slice(0, start) + emoji + body.slice(end);
    if (next.length > COMMENT_LIMIT) {
      setError("评论已达到字数上限");
      return;
    }
    setBody(next);
    setEmojiOpen(false);
    // 保留选择范围：表情替换选中文字，插入后把光标放回表情末尾。
    const cursor = start + emoji.length;
    selection.current = { start: cursor, end: cursor };
    requestAnimationFrame(() => {
      input.current?.focus();
      input.current?.setSelectionRange(cursor, cursor);
    });
  }
  useEffect(() => {
    if (parent) input.current?.focus();
  }, [parent]);
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!body.trim() || submitting.current) return;
    submitting.current = true;
    setPending(true);
    setError("");
    const result = await apiRequest<Comment>(COMMENTS_API, {
      method: "POST",
      body: JSON.stringify({
        targetType: "PUBLICATION",
        targetKey,
        body: body.trim(),
        parentId: parent?.id ?? null,
      }),
    });
    submitting.current = false;
    setPending(false);
    if (!result.ok) {
      setError(result.error.msg);
      return;
    }
    setBody("");
    await onSent();
    onCancel?.();
  }
  return (
    <form className="comment-composer" onSubmit={(event) => void submit(event)}>
      {parent && (
        <div className="comment-reply-to">
          <span>
            回复 <span className="comment-mention">@{parent.authorName}</span>
          </span>
          <button type="button" onClick={onCancel}>
            取消
          </button>
        </div>
      )}
      <textarea
        ref={input}
        aria-label={parent ? `回复 ${parent.authorName}` : "写下评论"}
        placeholder={
          parent ? "写下你的回复…" : "分享观点、提出问题，或聊聊你的实践…"
        }
        value={body}
        onChange={(event) => {
          setBody(event.target.value);
          selection.current = {
            start: event.target.selectionStart,
            end: event.target.selectionEnd,
          };
        }}
        onSelect={(event) => {
          selection.current = {
            start: event.currentTarget.selectionStart,
            end: event.currentTarget.selectionEnd,
          };
        }}
        maxLength={COMMENT_LIMIT}
        rows={3}
        disabled={pending}
      />
      <footer>
        <div className="comment-composer-tools">
          <Popover.Root
            open={emojiOpen}
            onOpenChange={(open) => {
              if (open && input.current)
                selection.current = {
                  start: input.current.selectionStart,
                  end: input.current.selectionEnd,
                };
              setEmojiOpen(open);
            }}
          >
            <Popover.Trigger
              type="button"
              className="comment-emoji-trigger"
              aria-label="添加表情"
              disabled={pending}
            >
              <Smile size={18} />
            </Popover.Trigger>
            <Popover.Portal>
              <Popover.Positioner
                side="top"
                align="start"
                sideOffset={10}
                className="comment-emoji-positioner"
              >
                <Popover.Popup
                  className="comment-emoji-popup"
                  finalFocus={input}
                >
                  <Popover.Title>选择表情</Popover.Title>
                  <div className="comment-emoji-grid">
                    {COMMENT_EMOJIS.map(([emoji, name]) => (
                      <button
                        type="button"
                        key={name}
                        aria-label={`表情 ${name}`}
                        onClick={() => insertEmoji(emoji)}
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                </Popover.Popup>
              </Popover.Positioner>
            </Popover.Portal>
          </Popover.Root>
          <span>
            {body.length.toLocaleString()} / {COMMENT_LIMIT.toLocaleString()}
          </span>
        </div>
        <button type="submit" disabled={!body.trim() || pending}>
          <Send size={14} />
          {pending ? "发送中…" : parent ? "发送回复" : "发表评论"}
        </button>
      </footer>
      {error && (
        <p className="comment-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

function CommentThread({
  root,
  targetKey,
  loginUrl,
  onChanged,
}: {
  root: RootComment;
  targetKey: string;
  loginUrl: string;
  onChanged: () => Promise<void>;
}) {
  const { user } = useAuthSession();
  const [replyTo, setReplyTo] = useState<Comment>();
  const [replies, setReplies] = useState<ReplyPage>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const loadingRef = useRef(false);
  async function moreReplies() {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    setError("");
    const page = replies ? replies.page + 1 : 1;
    const result = await apiRequest<ReplyPage>(
      `${COMMENTS_API}/${root.comment.id}/replies?page=${page}&size=${PAGE_SIZE}`,
    );
    loadingRef.current = false;
    setLoading(false);
    if (!result.ok) {
      setError(result.error.msg);
      return;
    }
    setReplies((previous) => ({
      ...result.data,
      items: [...(previous?.items ?? []), ...result.data.items],
    }));
  }
  const rows = replies?.items ?? root.previewReplies;
  return (
    <article className="comment-thread">
      <CommentRow
        comment={root.comment}
        loginUrl={loginUrl}
        onReply={() => setReplyTo(root.comment)}
        onDeleted={onChanged}
      />
      {rows.length > 0 && (
        <div className="comment-replies">
          {rows.map((comment) => (
            <CommentRow
              key={comment.id}
              comment={comment}
              loginUrl={loginUrl}
              onReply={() => setReplyTo(comment)}
              onDeleted={onChanged}
            />
          ))}
        </div>
      )}
      {root.replyCount > rows.length && (
        <button
          className="comment-more-replies"
          type="button"
          disabled={loading}
          onClick={() => void moreReplies()}
        >
          {loading
            ? "正在加载…"
            : replies
              ? "查看更多回复"
              : `查看全部 ${root.replyCount} 条回复`}
        </button>
      )}
      {error && (
        <p className="comment-error" role="alert">
          {error}
          <button type="button" onClick={() => void moreReplies()}>
            重试
          </button>
        </p>
      )}
      {user && replyTo && (
        <CommentComposer
          key={replyTo.id}
          targetKey={targetKey}
          parent={replyTo}
          onSent={onChanged}
          onCancel={() => setReplyTo(undefined)}
        />
      )}
    </article>
  );
}

function CommentRow({
  comment,
  loginUrl,
  onReply,
  onDeleted,
}: {
  comment: Comment;
  loginUrl: string;
  onReply: () => void;
  onDeleted: () => Promise<void>;
}) {
  const { user } = useAuthSession();
  const [liked, setLiked] = useState(comment.likedByMe);
  const [count, setCount] = useState(comment.likeCount);
  const [pending, setPending] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [error, setError] = useState("");
  const busy = useRef(false);
  const row = useRef<HTMLDivElement>(null);
  const [likeFeedback, setLikeFeedback] = useState<{ enabled: boolean } | null>(
    null,
  );
  useGSAP(
    () => {
      if (!likeFeedback) return;
      // 只在服务端确认主动点赞后播放，尊重系统的减少动态效果设置。
      const media = gsap.matchMedia();
      media.add("(prefers-reduced-motion: no-preference)", () => {
        const timeline = gsap.timeline();
        timeline
          .fromTo(
            ".comment-like-symbol",
            {
              scale: likeFeedback.enabled ? 0.65 : 1.15,
            },
            {
              scale: likeFeedback.enabled ? 1.35 : 0.85,
              duration: 0.16,
              ease: "power2.out",
            },
          )
          .to(".comment-like-symbol", {
            scale: 1,
            duration: 0.4,
            ease: "elastic.out(1, 0.5)",
          })
          .fromTo(
            ".comment-like-count",
            { y: 4, opacity: 0.4 },
            { y: 0, opacity: 1, duration: 0.25 },
            0,
          );
        if (likeFeedback.enabled)
          timeline.fromTo(
            ".comment-like-ring",
            { scale: 0.5, opacity: 0.65 },
            { scale: 2.1, opacity: 0, duration: 0.45, ease: "power2.out" },
            0,
          );
      });
      return () => media.revert();
    },
    { scope: row, dependencies: [likeFeedback], revertOnUpdate: true },
  );
  async function act(remove: boolean) {
    if (busy.current) return;
    busy.current = true;
    setPending(true);
    setError("");
    const result = await apiRequest<{ liked: boolean }>(
      `${COMMENTS_API}/${comment.id}${remove ? "" : "/like"}`,
      { method: remove ? "DELETE" : "POST" },
    );
    busy.current = false;
    setPending(false);
    if (!result.ok) {
      setError(result.error.msg);
      return;
    }
    if (remove) {
      await onDeleted();
      return;
    }
    setLiked(result.data.liked);
    setLikeFeedback({ enabled: result.data.liked });
    setCount((value) =>
      Math.max(
        0,
        value + (result.data.liked === liked ? 0 : result.data.liked ? 1 : -1),
      ),
    );
  }
  return (
    <div ref={row} className="comment-row">
      <UserAvatar
        className="comment-avatar"
        name={comment.authorName}
        url={comment.authorAvatarUrl}
        tone={Number(comment.authorId.slice(-1)) % 4}
      />
      <div className="comment-content">
        <header>
          <strong>{comment.authorName}</strong>
          <time dateTime={comment.createdAt}>
            {new Date(comment.createdAt).toLocaleDateString("zh-CN", {
              month: "short",
              day: "numeric",
            })}
          </time>
        </header>
        <p className={comment.deleted ? "comment-deleted" : undefined}>
          {!comment.deleted &&
            comment.parentId &&
            comment.parentId !== comment.rootId && (
              <span className="comment-reply-prefix">
                回复{" "}
                <span className="comment-mention">
                  @{comment.replyToAuthorName ?? "社区成员"}
                </span>
                ：{" "}
              </span>
            )}
          {comment.deleted ? "这条评论已删除" : comment.body}
        </p>
        {!comment.deleted && (
          <div className="comment-actions">
            {user ? (
              <>
                <button
                  type="button"
                  aria-label={`点赞 ${comment.authorName} 的评论`}
                  aria-pressed={liked}
                  disabled={pending}
                  onClick={() => void act(false)}
                >
                  <span className="comment-like-icon" aria-hidden="true">
                    <span className="comment-like-ring" />
                    <Heart
                      className="comment-like-symbol"
                      size={14}
                      fill={liked ? "currentColor" : "none"}
                    />
                  </span>
                  <span className="comment-like-count">{count || "赞"}</span>
                </button>
                <button type="button" onClick={onReply}>
                  回复
                </button>
              </>
            ) : (
              <>
                <Link href={loginUrl}>
                  <Heart size={14} />
                  {count || "赞"}
                </Link>
                <Link href={loginUrl}>回复</Link>
              </>
            )}
            {comment.canDelete &&
              (confirmDelete ? (
                <span className="comment-delete-confirm">
                  删除这条评论？
                  <button
                    type="button"
                    className="comment-confirm-delete"
                    disabled={pending}
                    onClick={() => void act(true)}
                  >
                    确认删除
                  </button>
                  <button type="button" onClick={() => setConfirmDelete(false)}>
                    取消
                  </button>
                </span>
              ) : (
                <button
                  type="button"
                  className="comment-delete"
                  aria-label={`删除 ${comment.authorName} 的评论`}
                  onClick={() => setConfirmDelete(true)}
                >
                  <Trash2 size={13} />
                </button>
              ))}
          </div>
        )}
        {error && (
          <p className="comment-error" role="alert">
            {error}
          </p>
        )}
      </div>
    </div>
  );
}
