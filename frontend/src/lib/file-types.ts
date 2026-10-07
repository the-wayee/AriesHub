/** 浏览器没有 MIME 常量枚举；集中维护上传声明，服务端仍负责最终格式与权限校验。 */
export const FILE_MIME_TYPES = {
  PNG: "image/png",
  JPEG: "image/jpeg",
  WEBP: "image/webp",
  MP4: "video/mp4",
  WEBM: "video/webm",
} as const;
export const IMAGE_MIME_TYPES: readonly string[] = [
  FILE_MIME_TYPES.PNG,
  FILE_MIME_TYPES.JPEG,
  FILE_MIME_TYPES.WEBP,
];
export const IMAGE_FILE_ACCEPT = IMAGE_MIME_TYPES.join(",");
export const KIBIBYTE = 1024;
export const MEBIBYTE = KIBIBYTE * KIBIBYTE;
export const AVATAR_MAX_BYTES = 5 * MEBIBYTE;
const PUBLICATION_IMAGE_MAX_BYTES = 10 * MEBIBYTE;
/** 前端校验与文件选择器共用同一配置，避免封面和正文上传限制漂移。 */
export const PUBLICATION_MEDIA = {
  COVER: { maxBytes: PUBLICATION_IMAGE_MAX_BYTES, accept: IMAGE_FILE_ACCEPT },
  IMAGE: { maxBytes: PUBLICATION_IMAGE_MAX_BYTES, accept: IMAGE_FILE_ACCEPT },
  VIDEO: {
    maxBytes: 100 * MEBIBYTE,
    accept: [FILE_MIME_TYPES.MP4, FILE_MIME_TYPES.WEBM].join(","),
  },
  ATTACHMENT: {
    maxBytes: 20 * MEBIBYTE,
    accept: ".pdf,.zip,.txt,.csv,.json,.md,.docx,.xlsx,.pptx",
  },
} as const;
export type InlineMediaKind = Exclude<keyof typeof PUBLICATION_MEDIA, "COVER">;

export const ARTICLE_ATTACHMENT_LIMIT = 20;
export const ARTICLE_ATTACHMENT_ACCEPT = `${PUBLICATION_MEDIA.ATTACHMENT.accept},${IMAGE_FILE_ACCEPT}`;
