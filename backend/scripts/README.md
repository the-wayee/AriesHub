# 本地社区联调内容

2026-10-07 已在当前开发数据库发布 8 篇（ID 4–11），6 篇免费、2 个积分课程、3 篇精选，每篇有封面和正文流程图。此处保存的是原创练习材料，内容页注明联调用途，不冒充真实客户项目。`community-content-published.json` 仅记录当前环境的 ID，其他环境须重新上传并生成自己的结果清单。

- `community-content.json`：文章源稿，含主题、内容形式、价格和素材引用占位符。
- `community-assets/`：正文使用的原创步骤图 PNG。
- `editorial-covers/`：2026-10-07 生成的八张原创概念封面，按文章主题分别设计；`prompts.json` 保存内置 image_gen 的完整提示词。封面不是实际客户作品或工具截图。
- `community-content-before-editorial.json`：原联调稿件；`community-content-editorial-previous.json` 保存此次更新前的后台内容快照，便于恢复标题、正文和素材引用。
- `UpdateCommunityContent.java`：已发布 Demo 的显式更新工具，先核对八篇原稿，再通过媒体上传与内容更新接口落地，不新增文章、不改变文章 ID 与发布状态；遇到与原稿不同的用户编辑会停止。与 `PublishCommunityContent.java` 一起编译，使用已有后端运行时 classpath，参数为仓库路径和 `--apply`；需要运行中的本地 8080 后端、数据库、Redis、已编译的后端类和当前终端的 OSS 环境变量。
- `render-community-assets.cjs`：用 HTML/CSS 渲染配图；先安装前端现有依赖，必要时设置 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 再运行 `node backend/scripts/render-community-assets.cjs`。
- `Publish-CommunityContent.ps1` / `PublishCommunityContent.java`：显式执行的本地开发导入，不属于应用启动或 Flyway 迁移，不在测试中自动发布。

从仓库根目录执行 `./backend/scripts/Publish-CommunityContent.ps1`。需要 JDK 27、已有数据库和 Redis、正在运行的本地 8080 后端，以及当前终端中配置好的项目 OSS 环境变量。脚本另外启动一个临时 Spring 上下文，为已有的启用管理员生成本次专用会话，再调用真实素材、创建和发布 HTTP 接口；结束后撤销该会话，不改密码，不撤销既有会话。媒体按管理员身份校验归属和限流。

同标题的既有内容会跳过，不覆盖用户编辑，不自动发布已有草稿；请先检查发布结果里的状态。导入按篇落地，失败时已发布内容保持可用，结果文件逐篇保存。重新执行前应先检查可能存在的草稿，避免把失败恢复当作原子事务。发布结果只存 ID 和素材 ID，不存凭据或临时签名 URL。

主页接口盘点和下一阶段顺序见 `doc/PROJECT.md` 第 11 节。当前首页和探索页已经读取真实发布接口；发布到数据库的内容会按筛选、排序和分页展示。
