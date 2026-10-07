"use client";
import { CategoryCreateFields } from "./admin-category-manager";
import Link from "next/link";
import { ArticleAttachmentEditor } from "./article-attachment-editor";
import type { ArticleAttachment } from "./publication-attachments";
import { TRIAL_BOUNDARY, migrateTrialBody } from "@/lib/publication-trial";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";
import { Dialog } from "@base-ui/react/dialog";
import { useRouter } from "next/navigation";
import {
  useEffect,
  useRef,
  useState,
  useCallback,
  type FormEvent,
} from "react";
import {
  ArrowLeft,
  Eye,
  PenLine,
  Save,
  Upload,
  ImagePlus,
  X,
  Check,
  PanelRightClose,
  PanelRightOpen,
} from "lucide-react";
import {
  adminRequest,
  type AdminPublicationDetail,
  type AdminCategory,
} from "@/lib/admin";
import { IMAGE_MIME_TYPES, PUBLICATION_MEDIA } from "@/lib/file-types";
import { RichEditor } from "./rich-editor";
import { Markdown } from "./markdown";
import { PublicationMedia } from "./publication-media";
import { useBeforeUnload } from "@/lib/use-before-unload";
import { uploadRequest, type UploadProgress as Progress } from "@/lib/upload";
import { UploadProgress } from "./upload-progress";
import { PageSkeleton } from "./page-skeleton";
gsap.registerPlugin(useGSAP);

interface Draft {
  title: string;
  summary: string;
  categoryId: string;
  publicationType: string;
  accessType: string;
  creditPrice: number;
  fullMarkdown: string;
  coverFileId: string | null;
  featured: boolean;
  attachmentIds: string[];
}
const empty: Draft = {
  title: "",
  summary: "",
  categoryId: "",
  publicationType: "ARTICLE",
  accessType: "FREE",
  creditPrice: 0,
  fullMarkdown: "",
  coverFileId: null,
  featured: false,
  attachmentIds: [],
};
function fromDetail(d: AdminPublicationDetail): Draft {
  const result = Object.fromEntries(
    Object.keys(empty).map((k) => [
      k,
      d[k as keyof AdminPublicationDetail] ?? empty[k as keyof Draft],
    ]),
  ) as unknown as Draft;
  result.fullMarkdown = migrateTrialBody(
    result.fullMarkdown,
    d.previewMarkdown ?? "",
    result.accessType === "CREDIT",
  );
  return result;
}
export function AdminPublicationEditor({ id }: { id?: string }) {
  const router = useRouter();
  const [draft, setDraft] = useState<Draft>(empty);
  const [attachments, setAttachments] = useState<ArticleAttachment[]>([]);
  const [detail, setDetail] = useState<AdminPublicationDetail>();
  const [categories, setCategories] = useState<AdminCategory[]>();
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [pending, setPending] = useState(false);
  const [autoSaving, setAutoSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [coverProgress, setCoverProgress] = useState<Progress>();
  const [coverName, setCoverName] = useState("");
  const uploadController = useRef<AbortController | null>(null);
  useEffect(() => () => uploadController.current?.abort(), []);
  const [preview, setPreview] = useState(false);
  const [settings, setSettings] = useState(true);
  const [settingsAnimating, setSettingsAnimating] = useState(false);
  const writer = useRef<HTMLFormElement>(null);
  const previousCanvas = useRef<DOMRect | null>(null);
  const [saved, setSaved] = useState("");
  const [saveTime, setSaveTime] = useState("");
  const [autoError, setAutoError] = useState("");
  const [notice, setNotice] = useState("");
  const [leaveTo, setLeaveTo] = useState<string>();
  const file = useRef<HTMLInputElement>(null);
  const canvas = useRef<HTMLElement>(null);
  const current = useRef(draft);
  const busy = useRef(false);
  const { contextSafe } = useGSAP(
    () => {
      const before = previousCanvas.current;
      const document = canvas.current;
      if (!before || !document) return;
      previousCanvas.current = null;
      const after = document.getBoundingClientRect();
      // 先读取最终布局，再从旧宽度与位置过渡；结束后清除内联样式，继续响应窗口尺寸。
      gsap.fromTo(
        document,
        { width: before.width, x: before.left - after.left },
        {
          width: after.width,
          x: 0,
          duration: 0.38,
          ease: "power3.out",
          clearProps: "width,transform",
          onComplete: () => setSettingsAnimating(false),
        },
      );
      if (settings)
        gsap.fromTo(
          writer.current?.querySelector(".writer-settings") ?? [],
          { opacity: 0, x: 14 },
          {
            opacity: 1,
            x: 0,
            duration: 0.3,
            ease: "power2.out",
            clearProps: "opacity,transform",
          },
        );
    },
    { scope: writer, dependencies: [settings], revertOnUpdate: true },
  );
  function toggleSettings() {
    contextSafe(() => {
      if (settingsAnimating) return;
      if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        setSettings(!settings);
        return;
      }
      previousCanvas.current = canvas.current?.getBoundingClientRect() ?? null;
      setSettingsAnimating(true);
      if (settings) {
        // 等侧栏退出后再卸载，避免 React 条件渲染截断退出动画。
        gsap.to(writer.current?.querySelector(".writer-settings") ?? [], {
          opacity: 0,
          x: 14,
          duration: 0.16,
          ease: "power2.in",
          onComplete: () => setSettings(false),
        });
      } else setSettings(true);
    })();
  }
  useEffect(() => {
    current.current = draft;
  }, [draft]);
  const dirty = !!categories && JSON.stringify(draft) !== saved;
  useBeforeUnload(dirty || uploading);
  useEffect(() => {
    let active = true;
    void Promise.all([
      adminRequest<AdminCategory[]>("/categories"),
      id
        ? adminRequest<AdminPublicationDetail>(`/publications/${id}`)
        : Promise.resolve(null),
      id
        ? adminRequest<ArticleAttachment[]>(`/publications/${id}/attachments`)
        : Promise.resolve(null),
    ]).then(([c, d, resources]) => {
      if (!active) return;
      if (!c.ok) {
        setLoadError(c.error.msg);
        return;
      }
      if (d && !d.ok) {
        setLoadError(d.error.msg);
        return;
      }
      if (resources && !resources.ok) {
        setLoadError(resources.error.msg);
        return;
      }
      const attached = resources?.ok ? resources.data : [];
      setAttachments(attached);
      setCategories(c.data);
      const initial = d?.ok
        ? fromDetail(d.data)
        : {
            ...empty,
            categoryId: c.data[0]?.id ?? "",
          };
      initial.attachmentIds = attached.map((item) => item.id);
      if (d?.ok) setDetail(d.data);
      setDraft(initial);
      setSaved(JSON.stringify(initial));
      setLoadError("");
    });
    return () => {
      active = false;
    };
  }, [id, attempt]);
  function field<K extends keyof Draft>(name: K, value: Draft[K]) {
    setDraft((d) => ({ ...d, [name]: value }));
    setAutoError("");
  }
  const persist = useCallback(
    async (auto = false) => {
      if (busy.current || uploading) return null;
      // 保存请求绑定当前快照；请求期间的新输入仍保留为待保存状态。
      const snapshot = current.current;
      if (
        !snapshot.title.trim() ||
        !snapshot.summary.trim() ||
        !snapshot.categoryId
      ) {
        if (!auto) setError("请填写标题、摘要和所属主题");
        return null;
      }
      if (snapshot.accessType === "CREDIT" && snapshot.creditPrice <= 0) {
        if (!auto) setError("积分内容的价格必须大于 0");
        return null;
      }
      busy.current = true;
      setPending(true);
      setAutoSaving(auto);
      if (!auto) setError("");
      const payload = {
        ...snapshot,
        categoryId: Number(snapshot.categoryId),
        creditPrice:
          snapshot.accessType === "FREE" ? 0 : Number(snapshot.creditPrice),
      };
      const r = await adminRequest<AdminPublicationDetail>(
        id ? `/publications/${id}` : "/publications",
        { method: id ? "PUT" : "POST", body: JSON.stringify(payload) },
      );
      busy.current = false;
      setPending(false);
      setAutoSaving(false);
      if (!r.ok) {
        if (auto) setAutoError(r.error.msg);
        else setError(r.error.msg);
        return null;
      }
      setSaved(JSON.stringify(snapshot));
      setDetail(r.data);
      setSaveTime(
        new Date().toLocaleTimeString("zh-CN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
      );
      setAutoError("");
      if (!id) router.replace(`/admin/publications/${r.data.id}`);
      return r.data;
    },
    [id, router, uploading],
  );
  useEffect(() => {
    if (
      !id ||
      !dirty ||
      detail?.status !== "DRAFT" ||
      pending ||
      uploading ||
      autoError
    )
      return;
    const timer = setTimeout(() => {
      void persist(true);
    }, 5000);
    return () => clearTimeout(timer);
  }, [
    id,
    dirty,
    draft,
    detail?.status,
    pending,
    uploading,
    autoError,
    persist,
  ]);
  useEffect(() => {
    const keyboard = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "s") {
        e.preventDefault();
        void persist();
      }
    };
    window.addEventListener("keydown", keyboard);
    return () => window.removeEventListener("keydown", keyboard);
  }, [persist]);
  useEffect(() => {
    // 捕获站内导航，避免侧栏或返回链接在自动保存完成前丢失正文。
    const intercept = (e: MouseEvent) => {
      if (
        (!dirty && !uploading) ||
        e.defaultPrevented ||
        e.button !== 0 ||
        e.metaKey ||
        e.ctrlKey ||
        e.shiftKey ||
        e.altKey
      )
        return;
      const a = (e.target as Element).closest<HTMLAnchorElement>("a[href]");
      if (!a || a.target === "_blank" || a.hasAttribute("download")) return;
      const to = new URL(a.href, location.href);
      if (to.origin !== location.origin || to.pathname === location.pathname)
        return;
      e.preventDefault();
      e.stopPropagation();
      setLeaveTo(to.pathname + to.search);
    };
    document.addEventListener("click", intercept, true);
    return () => document.removeEventListener("click", intercept, true);
  }, [dirty, uploading]);
  async function submit(e: FormEvent) {
    e.preventDefault();
    await persist();
  }
  async function changeStatus(action: "publish" | "archive") {
    if (busy.current) return;
    if (action === "publish" && !current.current.fullMarkdown.trim()) {
      setError("请先写好完整正文，再发布内容");
      return;
    }
    const latest = dirty || !id ? await persist() : detail;
    if (!latest) return;
    busy.current = true;
    setPending(true);
    const r = await adminRequest<AdminPublicationDetail>(
      `/publications/${latest.id}/${action}`,
      { method: "POST" },
    );
    busy.current = false;
    setPending(false);
    if (r.ok) {
      setDetail(r.data);
      setError("");
      setNotice(action === "publish" ? "内容已发布" : "内容已下架");
    } else setError(r.error.msg);
  }
  async function cover(selected: File) {
    if (
      !IMAGE_MIME_TYPES.includes(selected.type) ||
      selected.size > PUBLICATION_MEDIA.COVER.maxBytes
    ) {
      setError("封面支持 PNG、JPEG、WebP，最大 10 MiB");
      return;
    }
    setUploading(true);
    setCoverName(selected.name);
    setCoverProgress({
      loaded: 0,
      total: null,
      percentage: null,
      phase: "uploading",
    });
    const controller = new AbortController();
    uploadController.current = controller;
    setError("");
    const body = new FormData();
    body.set("kind", "COVER");
    body.set("file", selected);
    const r = await uploadRequest<{ id: string }>(
      "/api/v1/admin/media",
      body,
      setCoverProgress,
      controller.signal,
    );
    setUploading(false);
    setCoverProgress(undefined);
    if (r.ok) field("coverFileId", r.data.id);
    else setError(r.error.msg);
  }
  if (loadError)
    return (
      <div className="ops-error" role="alert">
        {loadError}
        <button onClick={() => setAttempt(attempt + 1)}>重新加载</button>
      </div>
    );
  if (!categories || (id && !detail))
    return <PageSkeleton variant="editor" label="正在准备写作空间" />;
  const headings = draft.fullMarkdown
    .split("\n")
    .filter((l) => /^#{2,3} /.test(l))
    .map((l) => l.replace(/^#+ /, ""));
  return (
    <form
      className={`writer-page admin-editor ${settings ? "" : "writer-focused"}`}
      ref={writer}
      onSubmit={submit}
    >
      <div className="writer-header">
        <div>
          <Link className="writer-back" href="/admin/publications">
            <ArrowLeft size={16} />
            内容管理
          </Link>
          <div className="writer-title-row">
            <h1>{id ? "编辑内容" : "新建内容"}</h1>
            <span className="ops-status">
              {detail?.status === "PUBLISHED"
                ? "已发布"
                : detail?.status === "ARCHIVED"
                  ? "已下架"
                  : "草稿"}
            </span>
          </div>
          <p className="writer-save-status" aria-live="polite">
            {pending
              ? "正在保存…"
              : autoError
                ? `自动保存失败：${autoError}`
                : dirty
                  ? "有未保存的修改"
                  : saveTime
                    ? `${saveTime} 已保存`
                    : id
                      ? "所有修改已保存"
                      : "开始记录你的实践"}{" "}
            ·{" "}
            {id && detail?.status === "DRAFT"
              ? "修改后自动保存"
              : "Ctrl / ⌘ + S 保存"}
          </p>
        </div>
        <div className="writer-actions">
          {detail?.status === "PUBLISHED" && (
            <Link
              className="ops-secondary"
              href={`/publications/${detail.id}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              查看文章 <Eye size={16} />
            </Link>
          )}
          <button
            type="button"
            className="ops-secondary"
            aria-pressed={preview}
            disabled={uploading}
            onClick={() => setPreview(!preview)}
          >
            {preview ? <PenLine size={16} /> : <Eye size={16} />}
            {preview ? "继续编辑" : "阅读预览"}
          </button>
          <button
            type="button"
            className="ops-secondary writer-settings-toggle"
            aria-label="切换发布设置"
            aria-pressed={settings}
            disabled={settingsAnimating}
            aria-expanded={settings}
            onClick={toggleSettings}
          >
            {settings ? (
              <PanelRightClose size={18} />
            ) : (
              <PanelRightOpen size={18} />
            )}
            {settings ? "专注写作" : "发布设置"}
          </button>
          <button
            type="submit"
            className="ops-secondary"
            disabled={pending || uploading}
          >
            <Save size={16} />
            {pending
              ? "保存中…"
              : detail?.status === "PUBLISHED"
                ? "保存修改"
                : "保存草稿"}
          </button>
          <button
            type="button"
            className="ops-primary"
            disabled={pending || uploading}
            onClick={() =>
              void changeStatus(
                detail?.status === "PUBLISHED" ? "archive" : "publish",
              )
            }
          >
            <Upload size={16} />
            {detail?.status === "PUBLISHED" ? "下架" : "发布"}
          </button>
        </div>
      </div>
      {error && (
        <p className="ops-error" role="alert">
          {error}
        </p>
      )}
      {notice && (
        <p className="ops-notice" role="status">
          <Check size={16} />
          {notice}
        </p>
      )}
      <Dialog.Root
        open={!!leaveTo}
        onOpenChange={(open) => {
          if (!open && !pending) setLeaveTo(undefined);
        }}
      >
        <Dialog.Portal>
          <Dialog.Backdrop className="ops-dialog-backdrop" />
          <Dialog.Popup className="ops-dialog">
            <Dialog.Title>保留这次创作</Dialog.Title>
            <Dialog.Description>
              还有未保存的修改。可以先保存再离开，或继续编辑。
            </Dialog.Description>
            {error && <p role="alert">{error}</p>}
            <div>
              <button
                type="button"
                disabled={pending || uploading}
                onClick={() => {
                  if (leaveTo) router.push(leaveTo);
                }}
              >
                放弃修改并离开
              </button>
              <button type="button" onClick={() => setLeaveTo(undefined)}>
                继续编辑
              </button>
              <button
                type="button"
                className="ops-primary"
                disabled={pending || uploading}
                onClick={async () => {
                  const to = leaveTo;
                  const result = await persist();
                  if (result && to) router.push(to);
                }}
              >
                保存并离开
              </button>
            </div>
          </Dialog.Popup>
        </Dialog.Portal>
      </Dialog.Root>
      <div className="writer-layout">
        <section className="writer-canvas" ref={canvas}>
          <div className="writer-document-heading">
            <label className="writer-title-label">
              <span className="sr-only">标题</span>
              <input
                className="writer-title-input"
                aria-label="标题"
                name="title"
                placeholder="给你的内容起个好标题"
                value={draft.title}
                onChange={(e) => field("title", e.target.value)}
                maxLength={160}
                required
              />
            </label>
            <div className="writer-summary-caption">
              <span>内容摘要</span>
              <span>{draft.summary.length} / 500</span>
            </div>
            <textarea
              className="writer-summary-input"
              aria-label="内容摘要"
              name="summary"
              placeholder="用一两句话，告诉读者这篇内容的价值…"
              value={draft.summary}
              onChange={(e) => field("summary", e.target.value)}
              maxLength={500}
              rows={2}
              required
            />
          </div>
          <div className="writer-tabs">
            <strong>完整正文</strong>
            <span>
              {draft.accessType === "CREDIT"
                ? "分界线前可免费试读，之后解锁可见"
                : "免费文章，发布后可阅读全文"}
            </span>
          </div>
          {draft.accessType === "CREDIT" &&
            !draft.fullMarkdown.includes(TRIAL_BOUNDARY) && (
              <p className="writer-trial-hint">
                尚未设置试读范围。将光标放到正文中的合适位置，点击工具栏「试读到这里」；不设置则不开放正文试读。
              </p>
            )}
          {detail?.accessType === "CREDIT" &&
            detail.previewMarkdown?.trim() &&
            !detail.fullMarkdown.includes(TRIAL_BOUNDARY) && (
              <p className="writer-trial-hint">
                旧版试读已合并到正文开头，请检查分界线位置；保存后生效。
              </p>
            )}
          {preview ? (
            <div className="writer-reading-preview">
              {draft.coverFileId && (
                <PublicationMedia
                  id={draft.coverFileId}
                  key={draft.coverFileId}
                  signedUrl={
                    draft.coverFileId === detail?.coverFileId
                      ? detail?.cover?.url
                      : undefined
                  }
                  admin
                  label="文章封面"
                />
              )}
              <Markdown admin>
                {draft.fullMarkdown || "还没有正文。切换到编辑模式开始写作。"}
              </Markdown>
            </div>
          ) : (
            <RichEditor
              label="完整正文"
              trialEnabled={draft.accessType === "CREDIT"}
              value={draft.fullMarkdown}
              onChange={(v) => field("fullMarkdown", v)}
              disabled={(pending && !autoSaving) || uploading}
              onUploadStateChange={setUploading}
            />
          )}
          <ArticleAttachmentEditor
            items={attachments}
            paid={draft.accessType === "CREDIT"}
            disabled={pending || uploading}
            onUploading={setUploading}
            onChange={(items) => {
              setAttachments(items);
              field(
                "attachmentIds",
                items.map((item) => item.id),
              );
            }}
          />
          <div className="writer-document-footer">
            <span>
              {draft.fullMarkdown.replace(/\s/g, "").length.toLocaleString()} 字
              · 约 {Math.max(1, Math.ceil(draft.fullMarkdown.length / 500))}{" "}
              分钟阅读
            </span>
            <span>支持插入图片、视频与附件</span>
          </div>
        </section>
        {settings && (
          <aside className="writer-settings">
            <div className="writer-settings-heading">
              <h2>发布设置</h2>
              <span>整理好，再分享</span>
              {detail?.status === "PUBLISHED" && (
                <p className="writer-live-note">保存后将立即更新线上内容。</p>
              )}
            </div>
            <label className="writer-setting">
              内容形式
              <select
                aria-label="内容形式"
                name="publicationType"
                value={draft.publicationType}
                onChange={(e) => field("publicationType", e.target.value)}
              >
                <option value="CASE_STUDY">实战案例</option>
                <option value="ARTICLE">学习文章</option>
                <option value="COURSE">课程</option>
              </select>
              <small>
                案例记录过程与结果；学习文章分享方法；课程组织系统的学习路径。
              </small>
            </label>
            <label className="writer-setting">
              所属主题{" "}
              <i
                aria-hidden="true"
                className="category-color-marker"
                style={{
                  backgroundColor: categories.find(
                    (category) => category.id === draft.categoryId,
                  )?.color,
                }}
              />
              <select
                aria-label="所属主题"
                name="categoryId"
                value={draft.categoryId}
                onChange={(e) => field("categoryId", e.target.value)}
                required
              >
                <option value="" disabled>
                  选择分类
                </option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              <small>
                按文章涉及的方向归档，如 AI 编程、AI
                演示。与内容形式、免费或积分阅读无关。
              </small>
            </label>
            <details className="writer-category-create">
              <summary>没有合适的分类？创建分类</summary>
              <CategoryCreateFields
                onCreated={(category) => {
                  setCategories((previous) => [...(previous ?? []), category]);
                  field("categoryId", category.id);
                }}
              />
              <Link
                href="/admin/categories"
                target="_blank"
                rel="noopener noreferrer"
              >
                管理全部分类 ↗
              </Link>
            </details>
            <div className="writer-setting">
              <span>文章封面</span>
              {draft.coverFileId ? (
                <div className="writer-cover">
                  <PublicationMedia
                    id={draft.coverFileId}
                    key={draft.coverFileId}
                    signedUrl={
                      draft.coverFileId === detail?.coverFileId
                        ? detail?.cover?.url
                        : undefined
                    }
                    admin
                    label="文章封面"
                  />
                  <button
                    type="button"
                    aria-label="移除封面"
                    disabled={uploading}
                    onClick={() => field("coverFileId", null)}
                  >
                    <X size={16} />
                  </button>
                </div>
              ) : (
                <button
                  type="button"
                  className="writer-cover-empty"
                  onClick={() => file.current?.click()}
                  disabled={uploading}
                >
                  <ImagePlus size={25} />
                  <strong>{coverProgress ? "上传中…" : "上传文章封面"}</strong>
                  <small>PNG / JPEG / WebP · 10 MiB</small>
                </button>
              )}
              {draft.coverFileId && (
                <button
                  type="button"
                  className="writer-cover-replace"
                  disabled={uploading}
                  onClick={() => file.current?.click()}
                >
                  <ImagePlus size={15} />
                  {uploading ? "上传中…" : "更换封面"}
                </button>
              )}
              <input
                ref={file}
                hidden
                type="file"
                accept={PUBLICATION_MEDIA.COVER.accept}
                aria-label="上传封面文件"
                onChange={(e) => {
                  const selected = e.target.files?.[0];
                  if (selected) void cover(selected);
                  e.target.value = "";
                }}
              />
              {coverProgress && (
                <UploadProgress
                  filename={coverName}
                  progress={coverProgress}
                  onCancel={() => uploadController.current?.abort()}
                />
              )}
            </div>
            <div className="writer-setting writer-pricing">
              <span>阅读方式</span>
              <div className="writer-access-switch">
                <button
                  type="button"
                  aria-pressed={draft.accessType === "FREE"}
                  onClick={() => {
                    field("accessType", "FREE");
                    field(
                      "fullMarkdown",
                      draft.fullMarkdown.replaceAll(TRIAL_BOUNDARY, ""),
                    );
                    field("creditPrice", 0);
                  }}
                >
                  免费阅读
                </button>
                <button
                  type="button"
                  aria-pressed={draft.accessType === "CREDIT"}
                  onClick={() => field("accessType", "CREDIT")}
                >
                  积分解锁
                </button>
              </div>
              <input type="hidden" name="accessType" value={draft.accessType} />
              {draft.accessType === "CREDIT" && (
                <label>
                  积分价格
                  <input
                    aria-label="积分价格"
                    name="creditPrice"
                    type="number"
                    min={1}
                    step={1}
                    required
                    value={draft.creditPrice}
                    onChange={(e) =>
                      field("creditPrice", Number(e.target.value))
                    }
                  />
                </label>
              )}
              <p>
                积分内容可在正文中设置试读分界线，分界线后的内容与素材按权益开放。
              </p>
            </div>
            <label className="writer-featured">
              <input
                type="checkbox"
                checked={draft.featured}
                onChange={(e) => field("featured", e.target.checked)}
              />
              <span>
                设为精选内容<small>用于社区精选与内容推荐</small>
              </span>
            </label>
            <div className="writer-setting">
              <span>文章链接</span>
              <small>
                {detail
                  ? `/publications/${detail.id}`
                  : "首次保存后自动生成唯一链接"}
              </small>
            </div>
            <div className="writer-outline">
              <h3>
                {draft.publicationType === "COURSE" ? "课程章节" : "文章目录"}
              </h3>
              {headings.length ? (
                headings.map((h, i) => (
                  <button
                    type="button"
                    key={i}
                    onClick={() => {
                      // 只定位当前正文区，避免跳到侧栏标题；遵循系统的减少动态效果设置。
                      const target =
                        canvas.current?.querySelectorAll<HTMLElement>(
                          ".tiptap h2, .tiptap h3, .writer-reading-preview .prose h2, .writer-reading-preview .prose h3",
                        )[i];
                      target?.scrollIntoView({
                        behavior: window.matchMedia(
                          "(prefers-reduced-motion: reduce)",
                        ).matches
                          ? "instant"
                          : "smooth",
                        block: "center",
                      });
                    }}
                  >
                    <span>{String(i + 1).padStart(2, "0")}</span>
                    {h}
                  </button>
                ))
              ) : (
                <p>在正文中添加二级标题，自动整理阅读目录。</p>
              )}
            </div>
          </aside>
        )}
      </div>
    </form>
  );
}
