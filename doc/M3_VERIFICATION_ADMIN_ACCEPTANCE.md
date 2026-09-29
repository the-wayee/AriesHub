# M3 邮箱验证码与内容后台验收记录

日期：2026-09-28

## 本轮交付

- 复用本机 `127.0.0.1:6379` Redis，密码 `root`，不创建项目专属 Redis 容器。
- 注册先申请六位邮箱验证码，通过 Resend API 真实投递；登录仅用邮箱和密码。
- Redis 只保存验证码 BCrypt 摘要，键使用邮箱 SHA-256；验证码十分钟失效、六十秒内禁止重发、最多尝试五次，并在成功使用后一次性删除。
- 登录、注册及注册发码按来源地址和邮箱分别限流；邮件发送失败时回滚验证码与冷却键。
- `ADMIN_EMAILS` 中的邮箱首次注册为管理员。Sa-Token 对 `/api/v1/admin/**` 同时校验登录态和 `ADMIN` 角色。
- 内容后台支持查看全部状态案例、新建草稿、编辑公开预览和完整正文、发布及下架。公开接口不会返回草稿或下架案例。
- Next.js 增加验证码倒计时交互、后台列表、案例编辑器和管理员导航入口。

## 配置

```bash
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=root
RESEND_API_KEY=re_xxx
RESEND_FROM=AriesHub <hello@your-domain.com>
ADMIN_EMAILS=you@example.com
```

Resend Key 与发件地址不写入仓库。未配置时后端可以启动，申请验证码返回 `EMAIL_DELIVERY_FAILED`，并清理本次 Redis 状态。

## 验证结果

- 后端 18 项 PostgreSQL 与 Redis 集成测试通过。
- 覆盖验证码摘要存储、键名隐私、重发冷却、错误验证码、成功消费、普通用户 403、管理员草稿创建、发布后公开可见和下架后不可见。
- 前端 TypeScript、ESLint、Prettier 和生产构建通过。
- Playwright 在桌面与手机视口检查真实案例浏览、验证码表单状态、注册登录流程和内容后台编辑器。身份与后台在浏览器层使用 API 拦截，防止自动测试发送真实邮件；后端规则由集成测试验证。

## 当前边界

- 本机尚未配置 Resend API Key 和已验证发件地址，因此本轮没有执行真实收件箱投递验收。
- Sa-Token 登录会话仍在进程内；Redis 承载邮箱验证码和认证限流。生产多实例前需要接入 Sa-Token Redis 适配。
- 已有普通账号不会因为后来加入 `ADMIN_EMAILS` 自动提升权限，角色变更应通过受控迁移或后续用户管理功能执行。
- 管理后台当前编辑 Markdown 文本，资源上传、预览草稿、版本历史和订单管理在后续里程碑实现。
