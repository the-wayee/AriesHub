/** 与后端 PublicationTrial 保持一致；前端只负责编辑显示，公开权限由后端裁剪。 */
export const TRIAL_BOUNDARY = "<!-- arieshub:paid -->";
export function migrateTrialBody(full: string, preview: string, paid: boolean) {
  if (!paid || !preview.trim() || full.includes(TRIAL_BOUNDARY)) return full;
  const intro = preview.trim();
  return full.startsWith(intro)
    ? `${intro}\n\n${TRIAL_BOUNDARY}\n\n${full.slice(intro.length).trimStart()}`
    : `${intro}\n\n${TRIAL_BOUNDARY}\n\n${full}`;
}
