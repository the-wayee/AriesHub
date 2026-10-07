# 后端分层与开发约定

当前工程采用按业务边界组织的 DDD 模块化单体。现有业务模块为 `catalog`（Publication 内容目录，含积分价格）、`identity`（用户身份，含积分余额）、`discussion`（全局评论线程）和 `storage`（文件元数据与对象存储）。积分流水与内容解锁表已建立，应用模块将在下一阶段接入。保持一个 Spring Boot 应用，不为当前规模拆微服务或多个 Maven 模块。

## 目录和依赖

```text
com.aries.backend
├── BackendApplication
├── catalog
│   ├── interfaces/rest              # Controller、HTTP 请求参数
│   ├── application
│   │   ├── service                  # 用例编排、事务边界
│   │   ├── query                    # 查询条件
│   │   ├── view                     # 只读结果投影
│   │   └── port                     # 查询及外部能力端口
│   ├── domain
│   │   ├── model                    # 聚合与业务规则
│   │   └── repository               # 聚合仓储契约
│   └── infrastructure/persistence
│       ├── po                       # 数据库对象
│       ├── mapper                   # MyBatis-Plus / 显式 SQL
│       ├── converter                # PO → 领域对象
│       └── repository               # 仓储及查询端口的实现
├── identity                         # 与 catalog 相同的四层结构
│   ├── interfaces/rest              # 注册、登录、退出、当前用户
│   ├── application                  # 身份用例和安全视图
│   ├── domain                       # 用户聚合及仓储契约
│   └── infrastructure               # 用户持久化、Sa-Token、密码编码
├── discussion                       # 可挂载任意业务目标的评论与回复树
│   ├── interfaces/rest              # 公开读取、登录后评论
│   ├── application                  # 树组装、目标和身份端口
│   ├── domain                       # 线程、评论层级规则
│   └── infrastructure/persistence   # 评论写入仓储与只读投影
├── storage                          # 私有文件上传、归属与临时下载；S3 SDK 位于 infrastructure
├── composition                      # 唯一允许组合多个业务模块的适配层
├── activity                         # 社区事件事实、幂等写入与公开动态投影
└── shared
    ├── application/exception        # 与 HTTP 无关的业务失败
    ├── interfaces/rest              # 错误响应、请求追踪、健康接口
    └── infrastructure               # 审计字段自动填充、数据库就绪检查
```

- `interfaces → application → domain`；基础设施实现应用端口和领域仓储。业务模块不相互引用，`shared` 不依赖业务模块。确需跨模块读取时，由 `composition` 同时实现两侧端口。
- `domain` 不导入 Spring、HTTP 或 ORM 类型。Lombok 仅用于减少样板代码。
- 应用层不直接引用 Mapper、PO、基础设施实现或 Sa-Token；登录态通过 `SessionManager` 端口操作。
- `catalog` Controller 不访问数据库，不承担发布、收费、权益判断。
- `shared` 只放跨模块技术能力，不变成所有业务的杂物目录。`BusinessException` 和 `ErrorCode` 是共享契约，错误码枚举由各业务模块维护。健康检查是技术接口，允许直接调用技术探针，不人为建立健康领域。
- PO 不离开基础设施层。公开 API 返回经过选择的只读投影。

认证用例：`AuthController` → `AuthApplicationService` → `UserRepository`，负责注册、登录和退出。用户资料用例：`UserController` → `UserApplicationService` → `UserRepository`，负责当前用户查询、昵称与签名修改、头像上传、绑定和读取。两者共享 `identity` 边界和 `UserAccount` 聚合，头像不构成独立领域。认证接口位于 `/api/v1/auth/**`，当前用户与资料接口位于 `/api/v1/users/me/**`。邮箱在领域内以 `Email` 值对象表示，由构造器完成规范化和校验。注册通过 `EmailVerificationService` 验证 Resend 邮件中的一次性验证码，登录只验证邮箱和 BCrypt 密码。Redis 保存验证码摘要、限流窗口和 Sa-Token 登录态；`sa-token-redis-template` 使用现有 Spring Data Redis 连接，使会话可跨重启和多实例共享。`AuthTrafficGuard` 通过 Redis 原子计数按来源地址和规范化邮箱分别限制登录、注册、注册发码，超限返回 429；来源地址只取服务端连接地址，不信任客户端可伪造的转发头。Sa-Token 在基础设施层实现 `SessionManager`；密码摘要不进入接口视图。身份路由由 identity 配置保护，管理员路由由 catalog 自身配置保护，未登录统一返回 401，非管理员返回 403。

## 统一 HTTP 返回体

所有 JSON 接口（含健康检查、认证、用户、目录、评论与文件签名）显式返回接口层的 `Result<T>`：`{code, msg, data, traceId}`。应用与领域层继续返回业务视图，不依赖 HTTP 返回契约；Controller 和 `ApiExceptionHandler` 均直接返回 `Result<T>`，不再使用 `ResponseEntity` 包装。固定 HTTP 状态通过 `@ResponseStatus` 声明；业务异常的动态状态和 `Retry-After` 通过 `HttpServletResponse` 设置。文件接口的 `no-store` 由 `RequestIdFilter` 统一设置。

成功码为字符串 `SUCCESS`，失败码沿用所属模块的稳定业务码；`msg` 为用户可读提示。`data` 为业务数据或 `null`，四个字段始终存在。创建和异步受理保持 201/202；退出、删除、隐藏、锁帖由原 204 改为 200、`data: null`。失败保留 400/401/403/404/409/429/500/503 等 HTTP 状态，前端同时校验 HTTP 状态和成功码。

`RequestIdFilter` 为每次请求生成 UUID，写入请求属性、日志 MDC 的 `traceId` 和 `X-Trace-Id` 响应头，返回体使用同一编号。过滤器不信任客户端传入的编号，并在请求结束时清理 MDC；`X-Request-Id` 暂保留为同值兼容头。内部异常详情只记录到服务端日志。限流信息通过 `Retry-After` 头及 `data.retryAfterSeconds` 返回，避免增加顶层字段。

前端的 `src/lib/api.ts` 集中验证与解包契约，认证、用户、后台和服务端目录请求共用解析器；不接受旧的裸数据响应。完整端点契约见 `openapi.json`。

## 当前用例

评论：公开读取 `/api/v1/discussions/comments`，返回根评论分页，每条带回复总数与前两条预览；`GET /comments/{rootId}/replies` 分页展开全部回复；`POST /comments/{id}/like` 切换点赞。登录成员可发布根评论或回复，作者可删除自己的评论，管理员通过 `/api/v1/admin/discussions/**` 隐藏评论或锁定讨论。接口使用 `targetType + targetKey` 定位挂载对象，应用层按类型路由到对应的 `DiscussionTargetResolver`，`composition` 提供校验实现。

回复是单层的：所有回复 `depth = 1` 并共享 `root_id`，回复一条回复时只改变 `parent_id`。这样根评论可以独立分页，回复按 `root_id` 批量读取。`HIDDEN` 与 `DELETED` 语义不同——前者整条不可见，后者保留占位行、正文置空而其下回复继续展示。

`discussion` 不依赖 `identity`，也不在应用层引用 Sa-Token：登录态与角色经 `DiscussionIdentityProvider` 由 `composition` 桥接。`composition` 里每个类只做一件事：`DiscussionIdentityAdapter` 实现身份桥接；每种可评论对象各有一个 `DiscussionTargetResolver` 实现（目前只有 `PublicationDiscussionTarget`）。`DiscussionTargets` 在启动时按 `targetType()` 建立路由，同一类型重复注册会直接启动失败。给新对象加评论只需在 `composition` 新增一个解析器类。

内容详情：Controller → `CatalogQueryService` → `PublicationRepository` 加载 `Publication` → 聚合判断公开可见 → 查询端口返回公开摘要与预览。

免费正文：应用层先调用聚合的 `allowsPublicReading()`，然后调用受限正文查询。数据库 SQL 再检查已发布、交付可用、免费类型。两道检查共同保护正文；未接入权益前，对所有积分内容正文请求返回 403。

列表属于查询用例，通过只读投影查询端口获取分页数据，不为展示列表重复组装全部聚合。管理员写用例按“加载 `Publication` 聚合 → 调用 `create`、`edit`、`publish` 或 `archive` 行为 → 通过 `PublicationRepository.save` 持久化”执行；完整正文是发布前置条件。管理员列表和详情保留在 `AdminCatalogReadPort`。公开查询仍独立检查发布与交付状态。

## Lombok 约定

- 固定 **1.18.48**，显式配置 Maven 注解处理器，与 Java 27 配合使用。
- PO 使用 `@Getter`、`@Setter`，避免手写字段存取方法。
- 构造器注入使用 `@RequiredArgsConstructor`；日志使用 `@Slf4j`。
- 领域对象和应用查询可用 `@Value`、`@Builder`；不要为聚合开放任意 setter。
- 无行为的不可变 API 投影可以用 Java record，二者各司其职。
- 不在包含密码、token、付费正文的类型上自动生成会输出全部字段的 `toString`。
- IDE 需要启用注解处理，最终以 `./mvnw clean test` 为构建依据。

## MyBatis-Plus 约定

- 使用 **3.5.17 的 Spring Boot 4 starter**，不再同时引入 MyBatis 原始 starter。
- 简单单表查询用 `BaseMapper` 和 `LambdaQueryWrapper`。例如仓储按 ID 查询用 `selectById`；文章不提供 slug 查询或回退。
- 联表、统计和权限敏感投影使用 XML，明确列清单，便于审查；不为了避免 SQL 堆砌多次查询。
- 生产单表增删改查统一使用 `BaseMapper`、Lambda Wrapper 和 `Page`，不直接注入 `JdbcTemplate`，也不为普通筛选编写 XML/注解 SQL。联表和跨表统计投影集中在 XML；特殊原子语句见下述例外。测试夹具的原始 SQL 只用于构造数据库事实。
- 成员单表分页使用 MyBatis-Plus `Page` 与 PostgreSQL 分页拦截器，配套 `mybatis-plus-jsqlparser` 依赖；每页最多 100 条，越界页返回空列表。既有联表投影保留绑定参数的 `LIMIT/OFFSET`，不在仓储拼接 SQL。
- 用户输入必须参数绑定，不使用 `${}` 拼接 SQL；ILIKE 的 `%`、`_` 和转义符按普通字符处理。
- 限制页大小和查询超时；数据库变更通过 Flyway，生产禁止自动更新表结构。
- 单表增删改查在 Repository 里用 `BaseMapper` + Lambda Wrapper 完成，不在 Mapper 里手写 SQL。只有 Wrapper 表达不了的语句才用注解或 XML：列对列的自引用表达式（如 `like_count = like_count + ?` 的原子计数）、`ON CONFLICT` 幂等插入、联表与聚合投影。
- `update(null, wrapper)` **不会**触发 `updateFill`（它需要实体作为填充载体），`updated_at` 会停在旧值。只用 Wrapper 更新时，必须在同一个 Wrapper 里 `.setSql("updated_at = now()")`；传入实体的 `updateById` 则会自动填充。
- 不需要逻辑删除语义的表（如点赞，取消即删行）不要继承 `BasePO`，否则 `@TableLogic` 会把删除变成软删，唯一约束也只能退化成偏索引。
- PO 继承 `BasePO` 后，`MetaObjectHandler` 自动填充 `created_at`、`updated_at` 和 `is_deleted`；`@TableLogic` 让普通查询自动排除已删除记录，并将删除转换为逻辑删除。Flyway 仍负责建立实际列、默认值、索引和约束。

## 常量与共用工具

- 优先使用现有库常量：PNG/JPEG MIME 使用 Spring `MimeTypeUtils`，multipart 声明使用 `MediaType`，字节单位使用 `DataSize`。
- 当前 Spring 未提供的 WebP/MP4/WebM MIME 值集中在 `shared/application/util/MediaTypes`；浏览器没有 MIME 常量枚举，前端集中在 `lib/file-types.ts`，文件选择与尺寸校验共享配置。
- 素材用途使用 `PublicationMediaKind` 枚举，账号角色/状态复用 `UserAccount` 现有枚举，不在业务方法中重复比较任意字面量。
- `FileSignatures` 只识别文件头；头像和文章素材复用此技术工具。尺寸、允许格式、限流、绑定和授权仍属于各自业务领域，不能回流到 `FileStorageService`。

## 中文注释

类注释说明职责与层次。方法注释重点解释发布规则、权限边界、金额和时区、事务、幂等及非直观实现原因。简单 getter/setter 交给 Lombok，不逐行翻译代码。

本轮关键注释覆盖：聚合访问规则、双重正文保护、SQL 通配符转义、请求编号和缓存限制、开发演示数据隔离。

## 验证

执行 `cd backend && ./mvnw clean test`。ArchUnit 测试检查模块隔离和分层依赖；聚合与邮箱值对象由纯单元测试覆盖；按模块拆分的集成测试使用 Testcontainers 创建独立 PostgreSQL 和 Redis，验证真实约束、SQL、MyBatis-Plus 仓储、验证码生命周期、管理员权限和 HTTP 响应，不接触开发服务。

包迁移、类重命名或 XML 重命名后使用 `clean` 清除旧 class 与资源，避免旧 Mapper 残留影响 Spring 启动。

参考：[Lombok 更新记录](https://projectlombok.org/changelog)、[MyBatis-Plus 安装说明](https://baomidou.com/getting-started/install/)。

## 对象存储

`storage` 通过 `ObjectStorage` 应用端口隔离 S3 SDK。`FileStorageService` 是文件读写、元数据和签名工具，不读取会话、不校验头像或附件业务规则。`identity/UserApplicationService` 负责头像格式、大小、文件头、限流、绑定及展示授权，经 `UserAvatarStorage` 与 composition 桥接；附件和视频以后由所属业务模块提供独立用例与上传入口。业务字段保存文件 ID；通用私有下载由 `FileDownloadService` 校验所有者。配置与接口说明见 [对象存储](OBJECT_STORAGE.md)。

## 成员内容关系与首页聚合（2026-10-07）

V12 新增 `publication_reactions` 与 `publication_reading_progress`。收藏和文章点赞通过唯一的 `(user_id, publication_id, kind)` 关系建模，取消时物理删除；阅读位置以 `(user_id, publication_id)` 唯一，保存正文版本、标题锚点、百分比和最后更新时间。它们独立于评论点赞、积分解锁和课程课时进度。

`PublicationReaderService` 校验当前账号、文章可见性和免费正文授权，写入前在事务中锁定文章行；关系 CRUD 使用 MyBatis-Plus，复杂卡片统计与个人分页由 XML 只读投影完成。文章行锁是首版明确的并发边界，热门文章大量写入时应评估按账号/文章更细粒度的串行化，而非无限增加接口并发。

`PublicationCardService` 批量读取互动、阅读位置与封面元数据。身份和存储分别通过 `PublicationReaderIdentity`、`PublicationCoverPort` 端口，由 composition 适配，catalog 不导入 identity/storage。FileStorageService 只提供通用批量元数据和签名方法。列表不返回全文；封面签名失败降级后，前端可通过已有受控单素材接口重试。

首页 Mock 社区活动属于前端展示层，没有虚构服务器事件或数据库成员记录；真实活动聚合应在发布/讨论事件及可见性规则明确后实现。所有新增 JSON 接口返回 Result，OpenAPI 为生成前端类型的唯一契约。

`PublicationReaderViews.PublicationCardView` 是文章卡片的只读返回 DTO，组合 `PublicationSummary`、封面签名、互动状态和阅读位置，不是持久化实体或新的业务领域。业务文件通过显式导入引用它，不能导入 JDK 智能卡包的 `javax.smartcardio.Card`。Java 局部变量均使用显式类型，具体约定见 `backend/AGENTS.md`。

分类管理的读取和创建统一归属 `AdminCategoryController`，由 `AdminCategoryService` 编排独立的 `CategoryReadPort` / `CategoryWritePort`。分类返回模型使用 `CategoryViews`，不再混入文章后台 DTO。文章用例通过分类读取端口校验引用，不承担分类创建职责。

跨领域 review 已收紧两处桥接：评论身份适配器通过 `IdentityDirectoryService` 查询角色和批量昵称；评论目标适配器通过 `CatalogQueryService` 查询公开可见性。领域仓储留在所属模块，`ArchitectureTest` 新增 composition 不直接依赖领域仓储的规则。素材适配器保留在 composition，以调用存储应用服务并转换文件模型；不把头像或文章规则放入通用存储服务。

## 哲学文案缓存（2026-10-07）

独立 `inspiration` 模块提供匿名 `GET /api/v1/inspiration/quote`，使用统一 Result，空池时 `data=null`。应用层依赖文案池/供应商端口，基础设施负责一言 HTTP 与 Redis。

启动一秒后后台预热，此后默认每 60 秒获取一条哲学文案（`c=k`），按正文去重，保留最近 60 条。Redis 的整池写入原子化且不设置 TTL，应用重启后恢复；内存不可变快照供用户随机读取，用户请求不访问 Redis 或上游。共享刷新租约在一个周期内最多允许一个实例拉取，即使上游失败也保留租约。上游超时、异常或 Redis 暂不可用不清空内存中的旧文案；首次预热失败时前端省略文案。

全项目环境变量：`INSPIRATION_ENABLED`（默认 true）、`INSPIRATION_REFRESH_MS`（默认 60000，最小 10000）、`INSPIRATION_POOL_SIZE`（默认 60，范围 1–500）。供应商连接超时 2 秒、请求超时 3 秒；前端只访问本站接口，不再实时请求一言。
