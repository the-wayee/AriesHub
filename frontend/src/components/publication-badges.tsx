import {
  BookOpen,
  CheckCircle2,
  Coins,
  Eye,
  Sparkles,
  Layers,
  GraduationCap,
} from "lucide-react";
import type { AccessType, PublicationType } from "@/lib/catalog-types";
import { CONTENT_FORM_LABELS } from "@/lib/publication-reader";

/** 固定业务状态统一使用语义色，文字与图标同时表达含义，不只依靠颜色。 */
export function PublicationFormBadge({
  type,
  minimal = false,
}: {
  type: PublicationType;
  minimal?: boolean;
}) {
  const Icon =
    type === "COURSE"
      ? GraduationCap
      : type === "CASE_STUDY"
        ? Layers
        : BookOpen;
  return (
    <span className="publication-badge" data-tone={type.toLowerCase()}>
      {minimal && <Icon aria-hidden="true" />}
      {CONTENT_FORM_LABELS[type]}
    </span>
  );
}

export function PublicationAccessBadge({
  access,
  credits,
  minimal = false,
}: {
  access: AccessType;
  credits: number;
  minimal?: boolean;
}) {
  return access === "FREE" ? (
    <span className="publication-badge" data-tone="free">
      {!minimal && <Sparkles aria-hidden="true" />}
      免费阅读
    </span>
  ) : (
    <span className="publication-access-badges">
      <span className="publication-badge" data-tone="credit">
        {!minimal && <Coins aria-hidden="true" />}
        <strong>{credits}</strong> 积分
      </span>
      {!minimal && (
        <span className="publication-badge" data-tone="preview">
          <Eye aria-hidden="true" />
          免费预览
        </span>
      )}
    </span>
  );
}

/** 百分比来自服务端；100% 表示已读完，继续保留重新阅读的入口。 */
export function PublicationProgressBadge({
  percent,
  label,
}: {
  percent: number;
  label?: string;
}) {
  const progress = Math.max(0, Math.min(100, Math.round(percent)));
  const complete = progress === 100;
  const Icon = complete ? CheckCircle2 : BookOpen;
  return (
    <span
      className="publication-badge publication-progress-badge"
      data-tone={complete ? "complete" : "reading"}
    >
      <Icon aria-hidden="true" />
      <span>{complete ? "已读完" : (label ?? "继续阅读 · 上次读到")}</span>
      <strong>
        {progress}
        <small>%</small>
      </strong>
    </span>
  );
}
