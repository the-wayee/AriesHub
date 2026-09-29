# AriesHub 独立站视觉概念（版式参考与当前配色）

> 2026-09-29 方向调整：前端原型继续对照这组稿子的构图与信息比例：并列的巨型字标与宣言、首页三栏主推案例、目录的宽图横排、详情的左侧成果画面与右侧价格栏。现行配色采用暖象牙白、深松绿、咖啡棕和少量旧铜色，呈现低调、克制、有材质感的视觉氛围。

这组图片是未来网站的视觉探索，不是已实现页面。图片中的人名、案例、价格、时间、订单、讨论和文件均为示意内容，开发时必须以真实业务数据与最终售卖条款为准。界面文案和细节需要在实现阶段重新校对。

设计依据：[项目规划](../../PROJECT.md)。产品主轴是主理人发布可复现的 AI 实战案例，用户按案例单次购买，随后阅读完整正文并下载资源。讨论和成员页面是社区发展方向，不替代付费内容的主入口。

## 页面总览

| 页面 | 概念图 | 对应路径/用途 | 阶段 |
| --- | --- | --- | --- |
| 落地页 | [01-landing.png](01-landing.png) | 外部流量进入 `/`，说明价值并引向案例 | 首版重点 |
| 个人主页 | [02-member-home.png](02-member-home.png) | 登录后的内容入口，继续阅读与已购案例 | 首版重点 |
| 案例目录 | [03-catalog.png](03-catalog.png) | `/cases`，查找免费和付费案例 | 首版重点 |
| 案例详情 | [04-case-detail.png](04-case-detail.png) | `/cases/[slug]`，展示成果、预览、交付与购买信息 | 首版重点 |
| 确认订单 | [05-checkout.png](05-checkout.png) | `/checkout/[caseId]`，核对内容、价格和条款 | 首版重点 |
| 案例阅读 | [06-reader.png](06-reader.png) | `/learn/[caseId]`，阅读正文和下载授权资源 | 首版重点 |
| 社区讨论 | [07-discussion.png](07-discussion.png) | 围绕案例提问、反馈和交流 | 后续社区能力 |
| 成员目录 | [08-members.png](08-members.png) | 认识主理人和分享实践的成员 | 后续社区能力 |
| 管理工作台 | [09-admin.png](09-admin.png) | `/admin` 及内容、资源、订单管理 | 首版重点，讨论管理为后续能力 |

核心路径：落地页 → 案例目录 → 案例详情/免费预览 → 确认订单 → 支付确认 → 案例阅读与下载 → 我的内容。社区讨论从案例详情和阅读页进入，成员页用于呈现参与者与作品。

## 视觉原则

- 克制的色彩系统；以字体、留白、细分隔线和内容画面建立层次，强调色服务于导航和重点行动。
- 页面像一本可操作的独立刊物：大标题、小号索引、错位网格与内容目录，而不是通用卡片式仪表盘。
- 案例结果图、交付清单、价格和免费预览始终优先；社区内容作为案例实践的延伸。
- 个人主页和后台延续同一视觉语言，同时保证阅读、下载、编辑与处理订单的操作清晰。

## 生成说明

原始九张黑白参考图使用 Codex 内置 `image_gen` 的 `ui-mockup` 模式逐页生成。它们提供版式参考，当前网站配色以实际前端实现为准。共同提示词约束为：`AriesHub; single-creator Chinese paid AI practice community; individual one-time case purchases; avant-garde minimal independent editorial website; strict pure black and white; Swiss asymmetric grid; strong typography; fine rules; grayscale project imagery; realistic navigable UI; no generic SaaS cards, gradients, colored accents, subscriptions, fake testimonials or metrics.` 各页分别补充上表所列的页面目标、内容结构与操作信息。

图片是设计讨论稿，不能直接当作前端截图或产品功能验收证据。尤其是文字生成可能出现错字、跨页案例信息不一致；开发时以确定后的设计规范和真实数据重建界面。

## 当前实施范围

当前前端使用 `frontend/src/lib/concept-cases.ts` 中的示例数据来验证设计，不依赖现有后端接口或数据表。四张彩色配图是概念插画，不代表案例的真实交付成果。`/`、`/cases`、案例详情、`/checkout/[slug]`、`/learn/[slug]`、`/community`、`/members`、`/my-content` 和 `/studio` 分别对应九张概念稿的前端原型。支付、订单、资源下载、讨论和内容管理操作尚未接入，页面会标示不可用的操作；未来以确定后的业务规则和真实数据替换示例。
