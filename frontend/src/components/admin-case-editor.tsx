"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  useEffect,
  useState,
  type FormEvent,
  type InputHTMLAttributes,
} from "react";
import {
  adminRequest,
  type AdminCaseDetail,
  type AdminCategory,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function AdminCaseEditor({ id }: { id?: string }) {
  const router = useRouter();
  const [categories, setCategories] = useState<AdminCategory[]>();
  const [detail, setDetail] = useState<AdminCaseDetail | null>(null);
  const [loadError, setLoadError] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  useEffect(() => {
    let active = true;
    const categoryRequest = adminRequest<AdminCategory[]>("/categories");
    const detailRequest = id
      ? adminRequest<AdminCaseDetail>(`/cases/${id}`)
      : Promise.resolve(null);
    void Promise.all([categoryRequest, detailRequest]).then(
      ([categoryResult, detailResult]) => {
        if (!active) return;
        if (!categoryResult.ok) {
          setLoadError(categoryResult.error.message);
          return;
        }
        setCategories(categoryResult.data);
        if (id && detailResult) {
          if (detailResult.ok) setDetail(detailResult.data);
          else setLoadError(detailResult.error.message);
        }
      },
    );
    return () => {
      active = false;
    };
  }, [id]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const body = Object.fromEntries(form.entries());
    const payload = {
      ...body,
      categoryId: Number(body.categoryId),
      priceMinor: Number(body.priceMinor),
    };
    const result = await adminRequest<AdminCaseDetail>(
      id ? `/cases/${id}` : "/cases",
      {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(payload),
      },
    );
    setPending(false);
    if (!result.ok) {
      setError(result.error.message);
      return;
    }
    setDetail(result.data);
    if (!id) router.replace(`/admin/cases/${result.data.id}`);
  }

  async function changeStatus(action: "publish" | "archive") {
    if (!id || pending) return;
    setPending(true);
    setError("");
    const result = await adminRequest<AdminCaseDetail>(
      `/cases/${id}/${action}`,
      {
        method: "POST",
      },
    );
    setPending(false);
    if (result.ok) setDetail(result.data);
    else setError(result.error.message);
  }

  if (loadError)
    return (
      <div className="admin-empty">
        <h2>{loadError}</h2>
      </div>
    );
  if (!categories || (id && !detail))
    return <p className="admin-loading">正在准备编辑器…</p>;

  return (
    <form className="admin-editor" onSubmit={save}>
      <div className="admin-editor-bar">
        <div>
          <Link className="text-link" href="/admin">
            ← 返回内容后台
          </Link>
          <h1>{id ? "编辑案例" : "新建案例"}</h1>
        </div>
        <div className="admin-actions">
          {detail?.status === "PUBLISHED" && (
            <Link
              className="quiet-button"
              href={`/cases/${detail.slug}`}
              target="_blank"
            >
              查看主站
            </Link>
          )}
          {id && detail?.status !== "PUBLISHED" && (
            <Button
              variant="outline"
              className="quiet-button"
              type="button"
              onClick={() => changeStatus("publish")}
            >
              发布
            </Button>
          )}
          {id && detail?.status === "PUBLISHED" && (
            <Button
              variant="outline"
              className="quiet-button"
              type="button"
              onClick={() => changeStatus("archive")}
            >
              下架
            </Button>
          )}
          <Button className="primary-link" type="submit" disabled={pending}>
            {pending ? "保存中…" : "保存内容"}
          </Button>
        </div>
      </div>
      {detail && (
        <p className="editor-status">
          当前状态：<strong>{detail.status}</strong> · 最后更新{" "}
          {new Intl.DateTimeFormat("zh-CN").format(new Date(detail.updatedAt))}
        </p>
      )}
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="editor-grid">
        <section className="editor-panel">
          <p className="eyebrow">BASIC INFORMATION</p>
          <h2>案例信息</h2>
          <EditorField
            label="标题"
            name="title"
            defaultValue={detail?.title}
            maxLength={160}
          />
          <EditorField
            label="页面地址"
            name="slug"
            defaultValue={detail?.slug}
            pattern="[a-z0-9-]{1,120}"
          />
          <label>
            内容分类
            <Select
              name="categoryId"
              defaultValue={detail?.categoryId?.toString() ?? ""}
              items={categories.map((category) => ({
                value: category.id.toString(),
                label: category.name,
              }))}
              required
            >
              <SelectTrigger aria-label="内容分类">
                <SelectValue placeholder="选择分类" />
              </SelectTrigger>
              <SelectContent>
                {categories.map((category) => (
                  <SelectItem key={category.id} value={category.id.toString()}>
                    {category.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </label>
          <label>
            案例摘要
            <Textarea
              name="summary"
              defaultValue={detail?.summary}
              maxLength={500}
              rows={4}
              required
            />
          </label>
          <div className="editor-two-columns">
            <label>
              阅读方式
              <Select
                name="accessType"
                defaultValue={detail?.accessType ?? "FREE"}
                items={[
                  { value: "FREE", label: "免费阅读" },
                  { value: "PAID", label: "付费案例" },
                ]}
                required
              >
                <SelectTrigger aria-label="阅读方式">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="FREE">免费阅读</SelectItem>
                  <SelectItem value="PAID">付费案例</SelectItem>
                </SelectContent>
              </Select>
            </label>
            <EditorField
              label="价格（分）"
              name="priceMinor"
              type="number"
              min={0}
              defaultValue={detail?.priceMinor ?? 0}
            />
          </div>
          <EditorField
            label="内容版本"
            name="version"
            defaultValue={detail?.version ?? "1.0"}
            maxLength={32}
          />
        </section>
        <section className="editor-panel editor-content-panel">
          <p className="eyebrow">CONTENT</p>
          <h2>正文与交付</h2>
          <label>
            公开预览（Markdown）
            <Textarea
              name="previewMarkdown"
              defaultValue={detail?.previewMarkdown}
              rows={10}
              required
            />
          </label>
          <label>
            完整正文（Markdown）
            <Textarea
              name="fullMarkdown"
              defaultValue={detail?.fullMarkdown}
              rows={18}
              required
            />
          </label>
          <label>
            开始之前
            <Textarea
              name="requirements"
              defaultValue={detail?.requirements}
              rows={4}
              required
            />
          </label>
          <label>
            内容与交付
            <Textarea
              name="deliverables"
              defaultValue={detail?.deliverables}
              rows={4}
              required
            />
          </label>
        </section>
      </div>
    </form>
  );
}

function EditorField({
  label,
  name,
  defaultValue,
  type = "text",
  ...props
}: {
  label: string;
  name: string;
  defaultValue?: string | number;
  type?: string;
} & Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "name" | "type" | "defaultValue"
>) {
  return (
    <label>
      {label}
      <Input
        name={name}
        type={type}
        defaultValue={defaultValue}
        required
        {...props}
      />
    </label>
  );
}
