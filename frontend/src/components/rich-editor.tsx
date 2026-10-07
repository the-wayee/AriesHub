"use client";
import {
  useEditor,
  EditorContent,
  ReactNodeViewRenderer,
  NodeViewWrapper,
  type NodeViewProps,
  useEditorState,
} from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import ImageExtension from "@tiptap/extension-image";
import { TableKit } from "@tiptap/extension-table";
import { TaskList, TaskItem } from "@tiptap/extension-list";
import { Markdown as MarkdownExtension } from "@tiptap/markdown";
import { useEffect, useRef, useState } from "react";
import {
  Bold,
  Italic,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Quote,
  Code2,
  ImagePlus,
  Video,
  Paperclip,
  Undo2,
  Redo2,
  Link as LinkIcon,
  Braces,
} from "lucide-react";
import { uploadRequest, type UploadProgress as Progress } from "@/lib/upload";
import { UploadProgress } from "./upload-progress";
import {
  PUBLICATION_MEDIA,
  MEBIBYTE,
  type InlineMediaKind,
} from "@/lib/file-types";
import { PublicationMedia } from "./publication-media";
function MediaNode({ node }: NodeViewProps) {
  const src = String(node.attrs.src ?? "");
  return (
    <NodeViewWrapper className="writer-media-node">
      {src.startsWith("media:") ? (
        <PublicationMedia
          id={src.slice(6)}
          admin
          label={String(node.attrs.alt ?? "文章图片")}
          kind={
            String(node.attrs.alt ?? "").startsWith("视频：")
              ? "VIDEO"
              : "IMAGE"
          }
        />
      ) : (
        <span>仅支持从素材工具上传图片</span>
      )}
    </NodeViewWrapper>
  );
}
const ContentImage = ImageExtension.extend({
  addNodeView() {
    return ReactNodeViewRenderer(MediaNode);
  },
});
export function RichEditor({
  value,
  onChange,
  label,
  disabled = false,
  onUploadStateChange,
}: {
  value: string;
  onChange: (value: string) => void;
  label: string;
  disabled?: boolean;
  onUploadStateChange?: (uploading: boolean) => void;
}) {
  const [inputKind, setInputKind] = useState<InlineMediaKind>("IMAGE");
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<Progress>();
  const [uploadName, setUploadName] = useState("");
  const uploadController = useRef<AbortController | null>(null);
  useEffect(() => () => uploadController.current?.abort(), []);
  const [error, setError] = useState("");
  const [source, setSource] = useState(false);
  const [linkDialog, setLinkDialog] = useState(false);
  const [link, setLink] = useState("");
  const file = useRef<HTMLInputElement>(null);
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [2, 3] },
        link: { openOnClick: false, protocols: ["media"] },
      }),
      ContentImage,
      // 与阅读器的 GFM 能力一致，避免打开、修改稿件时丢失表格与任务状态。
      TableKit,
      TaskList,
      TaskItem.configure({ nested: true }),
      MarkdownExtension,
    ],
    content: value,
    contentType: "markdown",
    immediatelyRender: false,
    editorProps: {
      attributes: {
        role: "textbox",
        "aria-label": label,
        "aria-multiline": "true",
        class: "writer-prose",
      },
    },
    onUpdate: ({ editor }) => onChange(editor.getMarkdown()),
  });
  const state = useEditorState({
    editor,
    selector: ({ editor: e }) => ({
      bold: e?.isActive("bold") ?? false,
      italic: e?.isActive("italic") ?? false,
    }),
  });
  useEffect(() => {
    if (editor && editor.getMarkdown() !== value)
      editor.commands.setContent(value, {
        contentType: "markdown",
        emitUpdate: false,
      });
  }, [editor, value]);
  useEffect(() => {
    editor?.setEditable(!disabled && !uploading);
  }, [editor, disabled, uploading]);
  async function upload(selected: File) {
    setError("");
    const limit = PUBLICATION_MEDIA[inputKind].maxBytes;
    if (selected.size > limit) {
      setError(`文件过大，最大 ${limit / MEBIBYTE} MiB`);
      return;
    }
    setUploading(true);
    setUploadName(selected.name);
    setUploadProgress({
      loaded: 0,
      total: null,
      percentage: null,
      phase: "uploading",
    });
    const controller = new AbortController();
    uploadController.current = controller;
    onUploadStateChange?.(true);
    const body = new FormData();
    body.set("file", selected);
    body.set("kind", inputKind);
    const result = await uploadRequest<{ id: string; filename: string }>(
      "/api/v1/admin/media",
      body,
      setUploadProgress,
      controller.signal,
    );
    setUploading(false);
    setUploadProgress(undefined);
    onUploadStateChange?.(false);
    if (!result.ok) {
      setError(result.error.msg);
      return;
    }
    const name = result.data.filename.replace(/[\[\]\r\n]/g, "");
    // 正文只保存稳定文件 ID；临时签名在展示时读取，避免文章链接过期。
    const url = `media:${result.data.id}`;
    if (source) {
      onChange(
        value +
          "\n\n" +
          (inputKind === "ATTACHMENT"
            ? `[附件：${name}](${url})`
            : `![${inputKind === "VIDEO" ? "视频：" : ""}${name}](${url})`),
      );
      return;
    }
    if (inputKind === "ATTACHMENT")
      editor
        ?.chain()
        .focus()
        .insertContent({
          type: "paragraph",
          content: [
            {
              type: "text",
              text: `附件：${name}`,
              marks: [{ type: "link", attrs: { href: url } }],
            },
          ],
        })
        .run();
    else
      editor
        ?.chain()
        .focus()
        .setImage({
          src: url,
          alt: inputKind === "VIDEO" ? `视频：${name}` : name,
        })
        .run();
  }
  const tools = [
    {
      name: "粗体",
      icon: Bold,
      active: state?.bold,
      action: () => editor?.chain().focus().toggleBold().run(),
    },
    {
      name: "斜体",
      icon: Italic,
      active: state?.italic,
      action: () => editor?.chain().focus().toggleItalic().run(),
    },
    {
      name: "二级标题",
      icon: Heading2,
      action: () => editor?.chain().focus().toggleHeading({ level: 2 }).run(),
    },
    {
      name: "三级标题",
      icon: Heading3,
      action: () => editor?.chain().focus().toggleHeading({ level: 3 }).run(),
    },
    {
      name: "项目列表",
      icon: List,
      action: () => editor?.chain().focus().toggleBulletList().run(),
    },
    {
      name: "编号列表",
      icon: ListOrdered,
      action: () => editor?.chain().focus().toggleOrderedList().run(),
    },
    {
      name: "引用",
      icon: Quote,
      action: () => editor?.chain().focus().toggleBlockquote().run(),
    },
    {
      name: "代码块",
      icon: Code2,
      action: () => editor?.chain().focus().toggleCodeBlock().run(),
    },
  ];
  return (
    <div className="rich-editor">
      <div
        className="writer-tools"
        role="toolbar"
        aria-label={`${label}格式工具`}
      >
        {tools.map((t) => (
          <button
            key={t.name}
            type="button"
            aria-label={t.name}
            title={t.name}
            aria-pressed={t.active}
            disabled={!editor || disabled || source}
            onMouseDown={(e) => e.preventDefault()}
            onClick={t.action}
          >
            <t.icon size={17} />
          </button>
        ))}
        <span className="writer-tool-divider" />
        <button
          type="button"
          aria-label="插入链接"
          title="插入链接"
          disabled={disabled || source}
          onClick={() => {
            setLink(editor?.getAttributes("link").href ?? "");
            setLinkDialog(!linkDialog);
          }}
        >
          <LinkIcon size={17} />
        </button>
        {(
          [
            { kind: "IMAGE", label: "插入图片", icon: ImagePlus },
            { kind: "VIDEO", label: "插入视频", icon: Video },
            { kind: "ATTACHMENT", label: "插入附件", icon: Paperclip },
          ] as const
        ).map((t) => (
          <button
            key={t.kind}
            type="button"
            aria-label={t.label}
            title={t.label}
            disabled={disabled || uploading}
            onClick={() => {
              setInputKind(t.kind);
              if (file.current) {
                file.current.accept = PUBLICATION_MEDIA[t.kind].accept;
                file.current.click();
              }
            }}
          >
            <t.icon size={17} />
          </button>
        ))}
        <span className="writer-tool-divider" />
        <button
          type="button"
          aria-label="撤销"
          disabled={!editor || disabled || source}
          onClick={() => editor?.chain().focus().undo().run()}
        >
          <Undo2 size={17} />
        </button>
        <button
          type="button"
          aria-label="重做"
          disabled={!editor || disabled || source}
          onClick={() => editor?.chain().focus().redo().run()}
        >
          <Redo2 size={17} />
        </button>
        <button
          type="button"
          aria-label="切换 Markdown 源码"
          title="Markdown 源码"
          aria-pressed={source}
          disabled={uploading}
          onClick={() => setSource(!source)}
        >
          <Braces size={17} />
        </button>
      </div>
      {linkDialog && (
        <div className="writer-link-form">
          <input
            aria-label="链接地址"
            type="url"
            value={link}
            onChange={(e) => setLink(e.target.value)}
            placeholder="https://"
          />
          <button
            type="button"
            onClick={() => {
              if (!/^https?:\/\//.test(link)) {
                setError("请输入 http 或 https 链接");
                return;
              }
              editor
                ?.chain()
                .focus()
                .extendMarkRange("link")
                .setLink({ href: link })
                .run();
              setLinkDialog(false);
              setError("");
            }}
          >
            插入
          </button>
          <button type="button" onClick={() => setLinkDialog(false)}>
            取消
          </button>
        </div>
      )}
      <input
        hidden
        ref={file}
        type="file"
        aria-label={`${label}素材上传`}
        onChange={(e) => {
          const selected = e.target.files?.[0];
          if (selected) void upload(selected);
          e.target.value = "";
        }}
      />
      {uploading && uploadProgress && (
        <UploadProgress
          filename={uploadName}
          progress={uploadProgress}
          onCancel={() => uploadController.current?.abort()}
        />
      )}
      {error && (
        <p className="ops-error" role="alert">
          {error}
        </p>
      )}
      {source ? (
        <textarea
          className="writer-source"
          aria-label={`${label} Markdown 源码`}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled || uploading}
        />
      ) : (
        <EditorContent editor={editor} />
      )}
    </div>
  );
}
