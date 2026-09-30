# 后端分层与开发约定

当前工程采用按业务边界组织的 DDD 模块化单体。现有业务模块为 `catalog`（Publication 内容目录，含积分价格）、`identity`（用户身份，含积分余额）和 `discussion`（全局评论线程）。积分流水与内容解锁表已建立，应用模块将在下一阶段接入。保持一个 Spring Boot 应用，不为当前规模拆微服务或多个 Maven 模块。

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
├── composition                      # 唯一允许组合多个业务模块的适配层
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

身份用例：Controller → `AuthApplicationService` → `UserRepository`。邮箱在领域内以 `Email` 值对象表示，由构造器完成规范化和校验。注册通过 `EmailVerificationService` 验证 Resend 邮件中的一次性验证码，登录只验证邮箱和 BCrypt 密码。Redis 保存验证码摘要、限流窗口和 Sa-Token 登录态；`sa-token-redis-template` 使用现有 Spring Data Redis 连接，使会话可跨重启和多实例共享。`AuthTrafficGuard` 通过 Redis 原子计数按来源地址和规范化邮箱分别限制登录、注册、注册发码，超限返回 429；来源地址只取服务端连接地址，不信任客户端可伪造的转发头。Sa-Token 在基础设施层实现 `SessionManager`；密码摘要不进入接口视图。身份路由由 identity 配置保护，管理员路由由 catalog 自身配置保护，未登录统一返回 401，非管理员返回 403。

## 当前用例

评论：公开读取 `/api/v1/discussions/comments`，返回根评论分页，每条带回复总数与前两条预览；`GET /comments/{rootId}/replies` 分页展开全部回复；`POST /comments/{id}/like` 切换点赞。登录成员可发布根评论或回复，作者可删除自己的评论，管理员通过 `/api/v1/admin/discussions/**` 隐藏评论或锁定讨论。接口使用 `targetType + targetKey` 定位挂载对象，应用层按类型路由到对应的 `DiscussionTargetResolver`，`composition` 提供校验实现。

回复是单层的：所有回复 `depth = 1` 并共享 `root_id`，回复一条回复时只改变 `parent_id`。这样根评论可以独立分页，回复按 `root_id` 批量读取。`HIDDEN` 与 `DELETED` 语义不同——前者整条不可见，后者保留占位行、正文置空而其下回复继续展示。

`discussion` 不依赖 `identity`，也不在应用层引用 Sa-Token：登录态与角色经 `DiscussionIdentityProvider` 由 `composition` 桥接。`DiscussionTargetResolver` 与 `DiscussionIdentityProvider` 都由 `composition/DiscussionContextAdapter` 实现。

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
- 简单单表查询用 `BaseMapper` 和 `LambdaQueryWrapper`。例如仓储按 ID 查询用 `selectById`，按 slug 查询使用 `PublicationPO::getSlug`。
- 联表、统计和权限敏感投影使用 XML，明确列清单，便于审查；不为了避免 SQL 堆砌多次查询。
- 生产代码和测试夹具统一通过 MyBatis-Plus Mapper 访问数据库，不直接注入 `JdbcTemplate`。简单 SQL 使用 `BaseMapper`、Lambda Wrapper 或 Mapper 注解；XML 只用于联表、动态条件和复杂投影。
- 本次联表分页使用绑定参数的 `LIMIT/OFFSET` 与相同条件的计数查询，不引入未使用的分页插件。
- 用户输入必须参数绑定，不使用 `${}` 拼接 SQL；ILIKE 的 `%`、`_` 和转义符按普通字符处理。
- 限制页大小和查询超时；数据库变更通过 Flyway，生产禁止自动更新表结构。
- 单表增删改查在 Repository 里用 `BaseMapper` + Lambda Wrapper 完成，不在 Mapper 里手写 SQL。只有 Wrapper 表达不了的语句才用注解或 XML：列对列的自引用表达式（如 `like_count = like_count + ?` 的原子计数）、`ON CONFLICT` 幂等插入、联表与聚合投影。
- `update(null, wrapper)` **不会**触发 `updateFill`（它需要实体作为填充载体），`updated_at` 会停在旧值。只用 Wrapper 更新时，必须在同一个 Wrapper 里 `.setSql("updated_at = now()")`；传入实体的 `updateById` 则会自动填充。
- 不需要逻辑删除语义的表（如点赞，取消即删行）不要继承 `BasePO`，否则 `@TableLogic` 会把删除变成软删，唯一约束也只能退化成偏索引。
- PO 继承 `BasePO` 后，`MetaObjectHandler` 自动填充 `created_at`、`updated_at` 和 `is_deleted`；`@TableLogic` 让普通查询自动排除已删除记录，并将删除转换为逻辑删除。Flyway 仍负责建立实际列、默认值、索引和约束。

## 中文注释

类注释说明职责与层次。方法注释重点解释发布规则、权限边界、金额和时区、事务、幂等及非直观实现原因。简单 getter/setter 交给 Lombok，不逐行翻译代码。

本轮关键注释覆盖：聚合访问规则、双重正文保护、SQL 通配符转义、请求编号和缓存限制、开发演示数据隔离。

## 验证

执行 `cd backend && ./mvnw clean test`。ArchUnit 测试检查模块隔离和分层依赖；聚合与邮箱值对象由纯单元测试覆盖；按模块拆分的集成测试使用 Testcontainers 创建独立 PostgreSQL 和 Redis，验证真实约束、SQL、MyBatis-Plus 仓储、验证码生命周期、管理员权限和 HTTP 响应，不接触开发服务。

包迁移、类重命名或 XML 重命名后使用 `clean` 清除旧 class 与资源，避免旧 Mapper 残留影响 Spring 启动。

参考：[Lombok 更新记录](https://projectlombok.org/changelog)、[MyBatis-Plus 安装说明](https://baomidou.com/getting-started/install/)。
