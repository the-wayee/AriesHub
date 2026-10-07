"use client";

import { useEffect, useRef, useState, type PointerEvent } from "react";
import { createPortal } from "react-dom";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";

gsap.registerPlugin(useGSAP);
const REORDER_DURATION = 0.32;
import { ArrowDown, ArrowUp, GripVertical, Pencil, Trash2 } from "lucide-react";
import type { AdminCategory } from "@/lib/admin";

type DragState = {
  category: AdminCategory;
  from: number;
  to: number;
  x: number;
  y: number;
  offsetX: number;
  offsetY: number;
  width: number;
  height: number;
  scrollY: number;
  listTop: number;
  listLeft: number;
  rects: DOMRect[];
  valid: boolean;
};

/** 保持原 DOM 顺序和指针捕获，用位移预览让位，松开后才保存最终顺序。 */
export function CategorySortableList({
  categories,
  disabled,
  onMove,
  onEdit,
  onDelete,
  onDragging,
}: {
  categories: AdminCategory[];
  disabled: boolean;
  onMove: (id: string, target: string) => void;
  onEdit: (category: AdminCategory) => void;
  onDelete: (category: AdminCategory) => void;
  onDragging: (dragging: boolean) => void;
}) {
  const list = useRef<HTMLUListElement>(null);
  const active = useRef<DragState | null>(null);
  const [preview, setPreview] = useState<DragState | null>(null);
  const isDragging = Boolean(preview);
  const previousPositions = useRef(new Map<string, number>());
  const motions = useRef(new Map<string, { offset: number }>());
  const skipDropAnimation = useRef(false);
  const orderKey = categories.map((category) => category.id).join(",");

  useGSAP(
    () => {
      if (!list.current) return;
      const rows = [
        ...list.current.querySelectorAll<HTMLElement>("[data-category-id]"),
      ];
      // 先批量读取布局，再写入动画；独立 translate 不干扰拖拽使用的 transform。
      const nextPositions = new Map(
        rows.map((row) => [row.dataset.categoryId!, row.offsetTop]),
      );
      const reduced = window.matchMedia(
        "(prefers-reduced-motion: reduce)",
      ).matches;
      for (const row of rows) {
        const id = row.dataset.categoryId!;
        const previous = previousPositions.current.get(id);
        const motion = motions.current.get(id) ?? { offset: 0 };
        gsap.killTweensOf(motion);
        const offset =
          previous === undefined
            ? 0
            : previous + motion.offset - nextPositions.get(id)!;
        motions.current.set(id, motion);
        if (reduced || skipDropAnimation.current || Math.abs(offset) < 0.5) {
          motion.offset = 0;
          row.style.removeProperty("translate");
          continue;
        }
        motion.offset = offset;
        row.style.translate = `0px ${offset}px`;
        gsap.to(motion, {
          offset: 0,
          duration: REORDER_DURATION,
          ease: "power2.out",
          onUpdate: () => {
            row.style.translate = `0px ${motion.offset}px`;
          },
          onComplete: () => {
            row.style.removeProperty("translate");
          },
        });
      }
      for (const [id, motion] of motions.current) {
        if (!nextPositions.has(id)) {
          gsap.killTweensOf(motion);
          motions.current.delete(id);
        }
      }
      previousPositions.current = nextPositions;
      skipDropAnimation.current = false;
    },
    { scope: list, dependencies: [orderKey] },
  );

  useEffect(() => {
    if (!isDragging) return;
    const previous = document.body.style.userSelect;
    document.body.style.userSelect = "none";
    function cancel(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      active.current = null;
      setPreview(null);
      onDragging(false);
    }
    window.addEventListener("keydown", cancel);
    return () => {
      document.body.style.userSelect = previous;
      window.removeEventListener("keydown", cancel);
    };
  }, [isDragging, onDragging]);

  function start(
    event: PointerEvent<HTMLButtonElement>,
    category: AdminCategory,
    index: number,
  ) {
    if (disabled || event.button !== 0 || !list.current) return;
    const rows = [
      ...list.current.querySelectorAll<HTMLElement>("[data-category-id]"),
    ];
    // 若箭头换位仍在播放，开始拖拽前归位，以统一拖拽测量坐标。
    for (const row of rows) {
      const motion = motions.current.get(row.dataset.categoryId!);
      if (motion) {
        gsap.killTweensOf(motion);
        motion.offset = 0;
      }
      row.style.removeProperty("translate");
    }
    const rects = rows.map((row) => row.getBoundingClientRect());
    const rect = rects[index];
    const bounds = list.current.getBoundingClientRect();
    event.currentTarget.focus({ preventScroll: true });
    event.currentTarget.setPointerCapture(event.pointerId);
    const state: DragState = {
      category,
      from: index,
      to: index,
      x: rect.left,
      y: rect.top,
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
      width: rect.width,
      height: rect.height,
      scrollY: window.scrollY,
      listTop: bounds.top,
      listLeft: bounds.left,
      rects,
      valid: true,
    };
    active.current = state;
    setPreview(state);
    onDragging(true);
  }

  function update(event: PointerEvent<HTMLButtonElement>) {
    const current = active.current;
    if (!current) return;
    // 触屏和鼠标共用指针事件；靠近视口边缘时滚动，原始测量加滚动量避免位移反馈抖动。
    const edge = 56;
    if (event.clientY < edge) window.scrollBy(0, -14);
    else if (event.clientY > window.innerHeight - edge) window.scrollBy(0, 14);
    const delta = window.scrollY - current.scrollY;
    const remaining = current.rects.filter(
      (_, index) => index !== current.from,
    );
    const to = remaining.filter(
      (rect) => event.clientY + delta > rect.top + rect.height / 2,
    ).length;
    const bottom = current.rects[current.rects.length - 1].bottom;
    const next = {
      ...current,
      to,
      x: event.clientX - current.offsetX,
      y: event.clientY - current.offsetY,
      valid:
        event.clientX >= current.listLeft - 24 &&
        event.clientX <= current.listLeft + current.width + 24 &&
        event.clientY + delta >= current.listTop - 32 &&
        event.clientY + delta <= bottom + 32,
    };
    active.current = next;
    setPreview(next);
  }

  function finish(cancel = false) {
    const current = active.current;
    active.current = null;
    setPreview(null);
    onDragging(false);
    if (!cancel && current?.valid && current.from !== current.to) {
      // 拖拽已有让位预览，落下时不重复播放；若后端失败，回滚仍会动画换位。
      skipDropAnimation.current = true;
      onMove(current.category.id, categories[current.to].id);
    }
  }

  const insertionTop = preview
    ? (preview.to > preview.from
        ? preview.rects[preview.to].bottom - preview.height
        : preview.rects[preview.to].top) - preview.listTop
    : 0;

  return (
    <>
      <div className="category-sort-stage">
        <ul
          ref={list}
          className="admin-category-list"
          aria-label="分类排序"
          aria-busy={disabled}
          data-sorting={isDragging}
        >
          {categories.map((category, index) => {
            const shift = !preview
              ? 0
              : preview.from < index && index <= preview.to
                ? -preview.height
                : preview.to <= index && index < preview.from
                  ? preview.height
                  : 0;
            return (
              <li
                key={category.id}
                data-category-id={category.id}
                data-dragging={preview?.category.id === category.id}
                style={{ transform: `translateY(${shift}px)` }}
              >
                <button
                  type="button"
                  className="category-drag-handle"
                  aria-label={`拖动排序 ${category.name}`}
                  disabled={disabled}
                  onPointerDown={(event) => start(event, category, index)}
                  onPointerMove={update}
                  onPointerUp={() => finish()}
                  onPointerCancel={() => finish(true)}
                  onLostPointerCapture={() => finish(true)}
                  onKeyDown={(event) => {
                    if (active.current) return;
                    const offset =
                      event.key === "ArrowUp"
                        ? -1
                        : event.key === "ArrowDown"
                          ? 1
                          : 0;
                    if (offset) {
                      event.preventDefault();
                      const target = categories[index + offset];
                      if (target) onMove(category.id, target.id);
                    }
                  }}
                >
                  <GripVertical size={18} />
                </button>
                <i
                  className="category-color-dot"
                  style={{ backgroundColor: category.color }}
                />
                <strong>{category.name}</strong>
                <span className="category-row-actions">
                  <button
                    type="button"
                    className="category-order-button"
                    disabled={disabled || isDragging || index === 0}
                    aria-label={`上移 ${category.name}`}
                    onClick={() =>
                      onMove(category.id, categories[index - 1].id)
                    }
                  >
                    <ArrowUp size={16} />
                  </button>
                  <button
                    type="button"
                    className="category-order-button"
                    disabled={
                      disabled || isDragging || index === categories.length - 1
                    }
                    aria-label={`下移 ${category.name}`}
                    onClick={() =>
                      onMove(category.id, categories[index + 1].id)
                    }
                  >
                    <ArrowDown size={16} />
                  </button>
                  <button
                    type="button"
                    className="category-order-button"
                    disabled={disabled || isDragging}
                    aria-label={`编辑 ${category.name}`}
                    onClick={() => onEdit(category)}
                  >
                    <Pencil size={16} />
                  </button>
                  <button
                    type="button"
                    className="category-order-button category-delete-button"
                    disabled={disabled || isDragging}
                    aria-label={`删除 ${category.name}`}
                    onClick={() => onDelete(category)}
                  >
                    <Trash2 size={16} />
                  </button>
                </span>
              </li>
            );
          })}
        </ul>
        {preview && preview.valid && (
          <div className="category-insertion-anchor" aria-hidden="true">
            <div
              className="category-insertion-slot"
              style={{ top: insertionTop, height: preview.height }}
            >
              <span>松开插入此处 · 第 {preview.to + 1} 位</span>
            </div>
          </div>
        )}
      </div>
      {preview &&
        createPortal(
          <div
            className="category-drag-overlay"
            aria-hidden="true"
            style={{
              width: preview.width,
              height: preview.height,
              transform: `translate3d(${preview.x}px, ${preview.y}px, 0)`,
            }}
          >
            <GripVertical size={18} />
            <i
              className="category-color-dot"
              style={{ backgroundColor: preview.category.color }}
            />
            <strong>{preview.category.name}</strong>
            <span>正在移动</span>
          </div>,
          document.body,
        )}
      <span className="sr-only" role="status">
        {preview
          ? `正在移动 ${preview.category.name}，插入第 ${preview.to + 1} 位，按 Escape 取消`
          : ""}
      </span>
    </>
  );
}
