# M2 身份基础验收记录

日期：2026-09-28

## 本轮交付

- 复用本机 `127.0.0.1:5432` PostgreSQL，在同一实例中使用独立 `arieshub` 数据库，不再启动项目专属数据库容器。
- 新增 `users` 表，保存规范化邮箱、BCrypt 密码摘要、昵称、角色、状态、邮箱验证状态和最近登录时间。
- 接入 Sa-Token 1.46.0，完成注册、登录、退出、当前用户和基础角色读取；浏览器登录凭证使用 HttpOnly、SameSite=Lax Cookie。
- 新增 `identity` DDD 模块，Controller 不直接访问 Mapper，密码摘要不进入 API 响应。
- 新增 Next.js 注册、登录、账号页及全站登录状态入口。
- 扩充首页，加入使用流程、案例入口和社区注册引导。

## 通用审计字段

`BasePO` 统一声明 `created_at`、`updated_at`、`is_deleted`：

- `MetaObjectHandler` 在插入时填充创建时间、更新时间和未删除状态，在更新时填充更新时间。
- `@TableLogic` 让 MyBatis-Plus 查询自动排除已删除记录，并将 `deleteById` 转换为逻辑删除。
- Flyway 迁移仍负责实际建列、默认值、非空约束和索引。这样批量 SQL 或其他受控写入没有经过 Java 框架时，数据库也有安全默认值。

## 已验证

- 后端 16 项 PostgreSQL 集成测试通过：注册、邮箱规范化、重复邮箱、BCrypt 摘要、正确/错误密码、Cookie 登录态、退出失效、未登录 401、审计字段和逻辑删除，以及原有案例权限规则。
- 前端 `typecheck`、`lint`、`format:check`、生产构建通过。
- Playwright 案例流程 14 项通过；身份注册、退出、重新登录在桌面与手机两种视口均通过。
- Flyway 已在本机 `arieshub` 数据库执行至 V2，并加载开发演示案例；Java 8080 和 Next.js 3000 联调可用。

## 上线前边界

- 本条是 M2 当时的边界；V3 已接入官方 Sa-Token Redis DAO，当前会话不再使用进程内存。
- 邮箱验证、找回密码、注册/登录限流、可信 Origin 与完整 CSRF 防护尚未实现。
- 当前只有普通用户自助流程；下一步增加管理员权限和内容管理后台。
