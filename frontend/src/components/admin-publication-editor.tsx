"use client";
import Link from "next/link";
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
import { RichEditor } from "./rich-editor";
import { Markdown } from "./markdown";
import { PublicationMedia } from "./publication-media";
import { useBeforeUnload } from "@/lib/use-before-unload";
interface Draft {
  title: string;
  slug: string;
  summary: string;
  categoryId: string;
  publicationType: string;
  accessType: string;
  creditPrice: number;
  previewMarkdown: string;
  fullMarkdown: string;
  requirements: string;
  deliverables: string;
  version: string;
  coverFileId: string | null;
  featured: boolean;
}
const empty: Draft = {
  title: "",
  slug: "",
  summary: "",
  categoryId: "",
  publicationType: "ARTICLE",
  accessType: "FREE",
  creditPrice: 0,
  previewMarkdown: "",
  fullMarkdown: "",
  requirements: "",
  deliverables: "",
  version: "1.0",
  coverFileId: null,
  featured: false,
};
function fromDetail(d: AdminPublicationDetail): Draft {
  return Object.fromEntries(
    Object.keys(empty).map((k) => [
      k,
      d[k as keyof AdminPublicationDetail] ?? empty[k as keyof Draft],
    ]),
  ) as unknown as Draft;
}
export function AdminPublicationEditor({ id }: { id?: string }) {
  const router = useRouter();
  const [draft, setDraft] = useState<Draft>(empty);
  const [detail, setDetail] = useState<AdminPublicationDetail>();
  const [categories, setCategories] = useState<AdminCategory[]>();
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [pending, setPending] = useState(false);
  const [autoSaving, setAutoSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [activeTab, setActiveTab] = useState<
    "fullMarkdown" | "previewMarkdown"
  >("fullMarkdown");
  const [preview, setPreview] = useState(false);
  const [settings, setSettings] = useState(true);
  const [saved, setSaved] = useState("");
  const [saveTime, setSaveTime] = useState("");
  const [autoError, setAutoError] = useState("");
  const [notice, setNotice] = useState("");
  const [leaveTo, setLeaveTo] = useState<string>();
  const file = useRef<HTMLInputElement>(null);
  const current = useRef(draft);
  const busy = useRef(false);
  useEffect(() => {
    current.current = draft;
  }, [draft]);
  const dirty = !!categories && JSON.stringify(draft) !== saved;
  useBeforeUnload(dirty);
  useEffect(() => {
    let active = true;
    void Promise.all([
      adminRequest<AdminCategory[]>("/categories"),
      id
        ? adminRequest<AdminPublicationDetail>(`/publications/${id}`)
        : Promise.resolve(null),
    ]).then(([c, d]) => {
      if (!active) return;
      if (!c.ok) {
        setLoadError(c.error.msg);
        return;
      }
      if (d && !d.ok) {
        setLoadError(d.error.msg);
        return;
      }
      setCategories(c.data);
      const initial = d?.ok
        ? fromDetail(d.data)
        : {
            ...empty,
            categoryId: c.data[0]?.id ?? "",
            slug: `ai-practice-${Date.now().toString(36)}`,
          };
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
      const snapshot = current.current;
      if (
        !snapshot.title.trim() ||
        !snapshot.summary.trim() ||
        !snapshot.categoryId ||
        !/^([a-z0-9-]{1,120})$/.test(snapshot.slug)
      ) {
        if (!auto) setError("请填写标题、摘要、分类和有效的页面地址");
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
    const intercept = (e: MouseEvent) => {
      if (
        !dirty ||
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
  }, [dirty]);
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
      !["image/png", "image/jpeg", "image/webp"].includes(selected.type) ||
      selected.size > 10 * 1024 * 1024
    ) {
      setError("封面支持 PNG、JPEG、WebP，最大 10 MiB");
      return;
    }
    setUploading(true);
    setError("");
    const body = new FormData();
    body.set("kind", "COVER");
    body.set("file", selected);
    const r = await adminRequest<{ id: string }>("/media", {
      method: "POST",
      body,
    });
    setUploading(false);
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
    return <p className="ops-loading">正在准备写作空间…</p>;
  const headings = draft.fullMarkdown
    .split("\n")
    .filter((l) => /^#{2,3} /.test(l))
    .map((l) => l.replace(/^#+ /, ""));
  return (
    <form
      className={`writer-page admin-editor ${settings ? "" : "writer-focused"}`}
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
                    : "开始记录你的实践"}{" "}
            ·{" "}
            {id && detail?.status === "DRAFT"
              ? "修改后自动保存"
              : "Ctrl / ⌘ + S 保存"}
          </p>
        </div>
        <div className="writer-actions">
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
            className="ops-icon-button writer-settings-toggle"
            aria-label="切换发布设置"
            aria-pressed={settings}
            onClick={() => setSettings(!settings)}
          >
            {settings ? (
              <PanelRightClose size={18} />
            ) : (
              <PanelRightOpen size={18} />
            )}
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
        <section className="writer-canvas">
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
          <div className="writer-tabs" role="tablist" aria-label="正文范围">
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "fullMarkdown"}
              disabled={uploading}
              onClick={() => setActiveTab("fullMarkdown")}
            >
              完整正文
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "previewMarkdown"}
              disabled={uploading}
              onClick={() => setActiveTab("previewMarkdown")}
            >
              公开预览
            </button>
            <span>
              {activeTab === "previewMarkdown"
                ? "所有访客可见"
                : draft.accessType === "CREDIT"
                  ? "仅对有权益的读者开放"
                  : "免费内容发布后可阅读"}
            </span>
          </div>
          {preview ? (
            <div className="writer-reading-preview">
              {activeTab === "fullMarkdown" && draft.coverFileId && (
                <PublicationMedia
                  id={draft.coverFileId}
                  admin
                  label="文章封面"
                />
              )}
              <Markdown admin>
                {draft[activeTab] || "还没有正文。切换到编辑模式开始写作。"}
              </Markdown>
            </div>
          ) : (
            <RichEditor
              key={activeTab}
              label={activeTab === "fullMarkdown" ? "完整正文" : "公开预览"}
              value={draft[activeTab]}
              onChange={(v) => field(activeTab, v)}
              disabled={(pending && !autoSaving) || uploading}
              onUploadStateChange={setUploading}
            />
          )}
          <div className="writer-document-footer">
            <span>
              {draft[activeTab].replace(/\s/g, "").length.toLocaleString()} 字 ·
              约 {Math.max(1, Math.ceil(draft[activeTab].length / 500))}{" "}
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
            </div>
            <label className="writer-setting">
              内容类型
              <select
                aria-label="内容类型"
                name="publicationType"
                value={draft.publicationType}
                onChange={(e) => field("publicationType", e.target.value)}
              >
                <option value="CASE_STUDY">实战案例</option>
                <option value="ARTICLE">学习文章</option>
                <option value="COURSE">课程</option>
              </select>
            </label>
            <label className="writer-setting">
              内容分类
              <select
                aria-label="内容分类"
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
            </label>
            <div className="writer-setting">
              <span>文章封面</span>
              {draft.coverFileId ? (
                <div className="writer-cover">
                  <PublicationMedia
                    id={draft.coverFileId}
                    admin
                    label="文章封面"
                  />
                  <button
                    type="button"
                    aria-label="移除封面"
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
                  <strong>{uploading ? "上传中…" : "上传文章封面"}</strong>
                  <small>PNG / JPEG / WebP · 10 MiB</small>
                </button>
              )}
              <input
                ref={file}
                hidden
                type="file"
                accept="image/png,image/jpeg,image/webp"
                aria-label="上传封面文件"
                onChange={(e) => {
                  const selected = e.target.files?.[0];
                  if (selected) void cover(selected);
                  e.target.value = "";
                }}
              />
            </div>
            <div className="writer-setting writer-pricing">
              <span>阅读方式</span>
              <div className="writer-access-switch">
                <button
                  type="button"
                  aria-pressed={draft.accessType === "FREE"}
                  onClick={() => {
                    field("accessType", "FREE");
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
              <p>公开预览始终免费；积分内容的完整正文和私有素材按权益开放。</p>
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
            <label className="writer-setting">
              页面地址
              <input
                aria-label="页面地址"
                name="slug"
                value={draft.slug}
                onChange={(e) => field("slug", e.target.value)}
                pattern="[a-z0-9-]{1,120}"
                required
              />
              <small>/publications/{draft.slug}</small>
            </label>
            <details className="writer-extra">
              <summary>交付说明与版本</summary>
              <label>
                开始之前
                <textarea
                  aria-label="开始之前"
                  value={draft.requirements}
                  onChange={(e) => field("requirements", e.target.value)}
                  rows={3}
                />
              </label>
              <label>
                内容与交付
                <textarea
                  aria-label="内容与交付"
                  value={draft.deliverables}
                  onChange={(e) => field("deliverables", e.target.value)}
                  rows={3}
                />
              </label>
              <label>
                内容版本
                <input
                  aria-label="内容版本"
                  value={draft.version}
                  onChange={(e) => field("version", e.target.value)}
                  maxLength={32}
                />
              </label>
            </details>
            <div className="writer-outline">
              <h3>
                {draft.publicationType === "COURSE" ? "课程章节" : "文章目录"}
              </h3>
              {headings.length ? (
                headings.map((h, i) => (
                  <p key={i}>
                    <span>{String(i + 1).padStart(2, "0")}</span>
                    {h}
                  </p>
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
