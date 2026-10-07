# 首页素材来源

记录日期：2026-09-29。图标逐文件下载地址及 SHA-256 见 frontend/public/brands/sources.json。所有运行时资源本地托管，无远端热链依赖。

## 产品图标

`frontend/public/brands/`，`frontend/src/lib/brand-icons.ts` 定义品牌列表。

- Lobe Icons：https://github.com/lobehub/lobe-icons ，目录 packages/static-svg/icons。Claude、OpenAI/ChatGPT、Gemini、Kimi、Ollama、DeepSeek、Qwen、豆包、Cursor、Perplexity、Midjourney、可灵、Suno、MiniMax、Runway、v0、GitHub、Notion、Figma 等使用现成 SVG。图标库许可原文见 brands/lobe-icons-LICENSE。
- Simple Icons：https://github.com/simple-icons/simple-icons ，TikTok 音符、小红书 SVG；许可原文见 brands/simple-icons-LICENSE。TikTok/Douyin 共享音符视觉，本项目标记为抖音。
- 图标仅识别讨论工具，商标属于各自权利人；不显示为合作、授权或已集成服务。21 种图标排成 30 张卡片，其中部分复用增加空间层次。

## AI 视频

- 作品：Ai Generated, Astronaut, Universe
- 作者：Frank_Rietsch
- 原页：https://pixabay.com/videos/ai-generated-astronaut-universe-257794/
- 公开下载文件：https://cdn.pixabay.com/video/2025/02/11/257794_large.mp4
- 页面标注：AI Generated，1920×1080，30 FPS，2025-02-12 发布。
- 使用依据：页面明确标注 Pixabay Content License：https://pixabay.com/service/license-summary/ ，允许在作品中使用与改编；不作为独立素材转售。首页播放器保留来源链接。
- 本地：frontend/public/media/ai-film.mp4，约 4.47 MB，10.43 秒。ai-film-poster.jpg 为 2 秒处提取封面。未更改视频内容。
- 这是第三方公开创作示例，不冒充社区成员原创作品，不代表本站提供视频生成服务。

## 画廊图片

沿用项目原有 frontend/public/concepts/*-color.webp 的生成概念素材，作为实践内容与认证画廊配图。不是品牌图标，不是作者真人照片。最终真实文章与作品由主理人替换；不使用 Cosmos 原站的作品作为项目素材。

## 开发社区内容配图（2026-10-07）

`backend/scripts/community-assets/01.png` 至 `08.png` 是本项目原创的黑白流程图，用 HTML/CSS 渲染，说明需求、边界、演示与自动化实践步骤；不取自 COSMOS，不使用真人照片。部分封面复用上述项目自有 `concepts/*-color.webp`。全部通过内容媒体接口上传到私有 OSS，并以 `media:UUID` 绑定文章；源码内容与发布 ID 清单保存在同目录 JSON 文件中。文章注明为原创练习与开发联调材料。

2026-10-07 编辑升级：八篇 Demo 封面已替换为 `backend/scripts/editorial-covers/01.png` 至 `08.png` 的原创概念影像，使用内置 image_gen 生成，完整提示词见 `prompts.json`。题材分别为需求光标、分层架构、验收清单、叙事织物、视觉书页、自动化传送带、知识花园和恢复迷宫；以玻璃、纸张与金属等材质形成统一的编辑方向。概念影像不代表真实工具界面或客户成果。原步骤图保留在正文，旧封面素材保持可恢复。

- 2026-10-07 成员首页示例头像：`community-widgets.tsx` 内原创 SVG 人物，三种头发、衣着与配色；仅用于标注为示例的 Mock 社区组件，不代表真实成员照片。
