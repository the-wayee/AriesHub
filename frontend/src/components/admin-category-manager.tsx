"use client";
import { useEffect, useState, useRef } from "react";
import { Plus } from "lucide-react";
import { CategorySortableList } from "./admin-category-sortable-list";
import { Button } from "./ui/button";
import { adminRequest, type AdminCategory } from "@/lib/admin";

/** 独立创建控件也可嵌入文章表单，不使用嵌套 form，不重置正在编辑的正文。 */
export function CategoryCreateFields({
  onCreated,
  onPendingChange,
}: {
  onCreated: (category: AdminCategory) => void;
  onPendingChange?: (pending: boolean) => void;
}) {
  const [name, setName] = useState("");
  const [color, setColor] = useState("#4967a9");
  const [order, setOrder] = useState("0");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  async function create() {
    if (pending) return;
    const sortOrder = Number(order);
    if (
      !name.trim() ||
      name.trim().length > 80 ||
      !order.trim() ||
      !Number.isInteger(sortOrder) ||
      sortOrder < 0 ||
      sortOrder > 10000
    ) {
      setError("请输入 1–80 字的分类名称，排序需为 0–10000 的整数。");
      return;
    }
    setPending(true);
    onPendingChange?.(true);
    setError("");
    const result = await adminRequest<AdminCategory>("/categories", {
      method: "POST",
      body: JSON.stringify({ name: name.trim(), sortOrder, color }),
    });
    setPending(false);
    onPendingChange?.(false);
    if (!result.ok) {
      setError(result.error.msg);
      return;
    }
    setName("");
    setOrder("0");
    onCreated(result.data);
  }
  return (
    <div
      className="admin-category-create"
      onKeyDown={(event) => {
        if (event.key === "Enter" && event.target instanceof HTMLInputElement) {
          event.preventDefault();
          void create();
        }
      }}
    >
      <label>
        分类名称
        <input
          aria-label="新分类名称"
          value={name}
          maxLength={80}
          disabled={pending}
          onChange={(event) => setName(event.target.value)}
          placeholder="例如：AI 视频创作"
        />
      </label>
      <label>
        显示排序
        <input
          aria-label="分类显示排序"
          type="number"
          min={0}
          max={10000}
          step={1}
          value={order}
          disabled={pending}
          onChange={(event) => setOrder(event.target.value)}
        />
      </label>
      <label>
        分类颜色
        <div className="category-color-field">
          <input
            aria-label="分类颜色"
            type="color"
            value={color}
            disabled={pending}
            onChange={(event) => setColor(event.target.value)}
          />
          <span>{color.toUpperCase()}</span>
          <span className="category-color-preview">
            <i style={{ backgroundColor: color }} />
            {name.trim() || "分类预览"}
          </span>
        </div>
      </label>
      <small>颜色用于分类标记；创建后可拖动调整顺序。</small>
      <Button type="button" disabled={pending} onClick={() => void create()}>
        <Plus size={16} />
        {pending ? "正在创建…" : "创建分类"}
      </Button>
      {error && (
        <p role="alert" className="reader-error">
          {error}
        </p>
      )}
    </div>
  );
}

export function AdminCategoryManager() {
  const [categories, setCategories] = useState<AdminCategory[]>();
  const [attempt, setAttempt] = useState(0);
  const [error, setError] = useState("");
  const [created, setCreated] = useState("");
  const [creating, setCreating] = useState(false);
  const [refreshing, setRefreshing] = useState(true);
  const [saving, setSaving] = useState(false);
  const [sortError, setSortError] = useState("");
  const [dragging, setDragging] = useState(false);
  const [editing, setEditing] = useState<AdminCategory | null>(null);
  const [deleting, setDeleting] = useState<AdminCategory | null>(null);
  const [actionError, setActionError] = useState("");
  const saveLock = useRef(false);

  /** 乐观更新只改变显示顺序；失败恢复快照，成功使用服务端的最终排序。 */
  async function move(id: string, target: string) {
    if (
      !categories ||
      creating ||
      refreshing ||
      saveLock.current ||
      id === target
    )
      return;
    const before = categories;
    const from = before.findIndex((category) => category.id === id);
    const to = before.findIndex((category) => category.id === target);
    if (from < 0 || to < 0) return;
    const next = [...before];
    next.splice(to, 0, next.splice(from, 1)[0]);
    saveLock.current = true;
    setSaving(true);
    setSortError("");
    setCategories(next);
    const result = await adminRequest<AdminCategory[]>("/categories/order", {
      method: "PUT",
      body: JSON.stringify({
        categoryIds: next.map((category) => category.id),
      }),
    });
    if (result.ok) {
      setCategories(result.data);
      setCreated("分类顺序已保存");
    } else {
      setCategories(before);
      setSortError(`${result.error.msg}，未保存本次排序。`);
    }
    setSaving(false);
    saveLock.current = false;
  }
  async function saveCategory() {
    if (!editing || saveLock.current) return;
    if (!editing.name.trim()) {
      setActionError("请输入分类名称");
      return;
    }
    saveLock.current = true;
    setSaving(true);
    setActionError("");
    const result = await adminRequest<AdminCategory>(
      `/categories/${editing.id}`,
      {
        method: "PUT",
        body: JSON.stringify({
          name: editing.name.trim(),
          color: editing.color,
        }),
      },
    );
    if (result.ok) {
      setCategories((previous) =>
        previous?.map((category) =>
          category.id === result.data.id ? result.data : category,
        ),
      );
      setEditing(null);
      setCreated("分类已更新");
    } else setActionError(result.error.msg);
    saveLock.current = false;
    setSaving(false);
  }
  async function deleteCategory() {
    if (!deleting || saveLock.current) return;
    saveLock.current = true;
    setSaving(true);
    setActionError("");
    const result = await adminRequest<null>(`/categories/${deleting.id}`, {
      method: "DELETE",
    });
    if (result.ok) {
      setCategories((previous) =>
        previous?.filter((category) => category.id !== deleting.id),
      );
      setDeleting(null);
      setCreated("分类已删除");
    } else setActionError(result.error.msg);
    saveLock.current = false;
    setSaving(false);
  }
  useEffect(() => {
    let active = true;
    void adminRequest<AdminCategory[]>("/categories").then((result) => {
      if (!active) return;
      setRefreshing(false);
      if (result.ok) {
        setCategories(result.data);
        setError("");
      } else setError(result.error.msg);
    });
    return () => {
      active = false;
    };
  }, [attempt]);
  return (
    <section className="admin-category-manager">
      <header>
        <p className="hub-kicker">内容组织</p>
        <h1>分类管理</h1>
        <p>为文章、课程及视频内容维护分类，首页与探索页使用同一份分类。</p>
      </header>
      <div className="admin-category-layout">
        <section className="admin-category-panel">
          <h2>创建分类</h2>
          <fieldset
            disabled={saving || dragging || Boolean(editing || deleting)}
            className="category-create-fieldset"
          >
            <CategoryCreateFields
              onPendingChange={setCreating}
              onCreated={(category) => {
                setCreated(`已创建「${category.name}」，编写文章时即可选择。`);
                setRefreshing(true);
                setAttempt((value) => value + 1);
              }}
            />
          </fieldset>
          {created && <p role="status">{created}</p>}
        </section>
        <section className="admin-category-panel">
          <h2>已有分类 {categories?.length ?? ""}</h2>
          {error ? (
            <div role="alert">
              <p>{error}</p>
              <Button onClick={() => setAttempt((value) => value + 1)}>
                重新加载
              </Button>
            </div>
          ) : !categories ? (
            <p role="status">正在读取分类…</p>
          ) : categories.length ? (
            <CategorySortableList
              categories={categories}
              disabled={
                saving || creating || refreshing || Boolean(editing || deleting)
              }
              onMove={(id, target) => void move(id, target)}
              onDragging={setDragging}
              onEdit={(category) => {
                setEditing({ ...category });
                setActionError("");
              }}
              onDelete={(category) => {
                setDeleting(category);
                setActionError("");
              }}
            />
          ) : (
            <p>还没有分类，先创建一个再开始编写内容。</p>
          )}
          {editing && (
            <form
              className="category-action-panel"
              aria-label="编辑分类"
              onSubmit={(event) => {
                event.preventDefault();
                void saveCategory();
              }}
            >
              <h3>
                编辑「
                {
                  categories?.find((category) => category.id === editing.id)
                    ?.name
                }
                」
              </h3>
              <label>
                分类名称
                <input
                  autoFocus
                  aria-label="编辑分类名称"
                  required
                  maxLength={80}
                  disabled={saving}
                  value={editing.name}
                  onChange={(event) =>
                    setEditing({ ...editing, name: event.target.value })
                  }
                />
              </label>
              <label>
                分类颜色
                <input
                  aria-label="编辑分类颜色"
                  type="color"
                  disabled={saving}
                  value={editing.color}
                  onChange={(event) =>
                    setEditing({ ...editing, color: event.target.value })
                  }
                />
              </label>
              {actionError && (
                <p role="alert" className="reader-error">
                  {actionError}
                </p>
              )}
              <div>
                <Button type="submit" disabled={saving}>
                  {saving ? "正在保存…" : "保存修改"}
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  disabled={saving}
                  onClick={() => setEditing(null)}
                >
                  取消
                </Button>
              </div>
            </form>
          )}
          {deleting && (
            <section
              className="category-action-panel"
              aria-label="删除分类确认"
            >
              <h3>删除「{deleting.name}」？</h3>
              <p>
                删除后将从分类列表移除。有关联内容的分类需要先迁移内容，才能删除。
              </p>
              {actionError && (
                <p role="alert" className="reader-error">
                  {actionError}
                </p>
              )}
              <div>
                <Button
                  autoFocus
                  type="button"
                  variant="ghost"
                  disabled={saving}
                  onClick={() => setDeleting(null)}
                >
                  取消
                </Button>
                <Button
                  type="button"
                  disabled={saving}
                  onClick={() => void deleteCategory()}
                >
                  {saving ? "正在删除…" : "确认删除"}
                </Button>
              </div>
            </section>
          )}
          <p className="category-sort-hint" role="status">
            {saving
              ? "正在保存顺序…"
              : "拖动左侧手柄排序，也可使用箭头按钮或键盘方向键。"}
          </p>
          {sortError && (
            <p role="alert" className="reader-error">
              {sortError}{" "}
              <button
                type="button"
                onClick={() => {
                  setSortError("");
                  setRefreshing(true);
                  setAttempt((value) => value + 1);
                }}
              >
                刷新列表
              </button>
            </p>
          )}
        </section>
      </div>
    </section>
  );
}
