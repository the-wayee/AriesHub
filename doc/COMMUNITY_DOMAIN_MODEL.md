# 社区、积分与内容模型决策

状态：2026-09-30，Publication 迁移已完成，积分应用继续实施。

## 1. 业务边界

AriesHub 是主理人持续发布原创内容、成员围绕实践交流的社区。内容是社区关系的中心，不把整个产品称为「案例库」。公开内容类型使用：

- `CASE_STUDY`：有完整过程与结果的实战案例；
- `ARTICLE`：观点、方法和短篇分享；
- `COURSE`：明确按章节组织的学习路径。

Java 聚合、数据库表、REST 路径、OpenAPI 和前端类型已统一迁移为 `Publication/publications`。历史 Flyway 迁移文件保留旧表名，V4 原位升级已有数据，不提供长期双写接口。

## 2. 评论作为独立 discussion 模块

评论不属于文章表，也不属于用户模块。`discussion` 只认识一个挂载目标：

```text
DiscussionTarget(type, key)
```

当前内容以 `PUBLICATION + 文章 ID 的十进制字符串` 挂载。以后课程章节、社区动态、作品或其他功能只需要新增一个 `DiscussionTargetResolver` 实现并声明自己的 `targetType()`，不需要复制评论表，也不必改动 discussion 或 composition。

`discussion_threads` 保证同一目标只有一个活动线程；`comments` 同时保存：

- `parent_id`：被回复的评论。回复一条回复时指向那条回复，界面据此显示「回复 @某人」；
- `root_id`：所属根评论，回复读取只按它走；
- `depth`：`0` 表示根评论，`1` 表示其下的回复；
- `status`：`PUBLISHED`、`HIDDEN`（管理员禁止展示）或 `DELETED`（作者自删）；
- `like_count`：反规范化点赞数，供「热门」「综合」排序使用。

**回复是单层的。** 根评论的三个字段为 `parent_id = null`、`root_id = null`、`depth = 0`；所有回复的 `depth` 恒为 `1` 并共享同一个 `root_id`，即使它回复的是另一条回复。这样根评论可以独立分页，其下回复按 `root_id` 一次取回，接口返回「回复总数 + 前两条预览」，前端不必处理任意深度的树，也不必递归组装。

早期设计允许无限嵌套并在应用层递归组装整棵树，递归深度等于回复链长度，是用户可构造的栈溢出；改为单层后这个风险消失，V3 里为分页准备但一直没用上的偏索引也按新查询形状重建。

**`HIDDEN` 与 `DELETED` 语义不同，不能合并成一个状态判断：**

- `HIDDEN`：整条不对外可见，读者看不到痕迹；
- `DELETED`：行作为占位保留，正文以空字符串返回并带 `deleted = true`，其下回复继续展示。

作者删除自己的根评论时**不会**连带删除下面的回复——回复是别人的发言，不该被一键抹掉。

目标表之间不建立多态数据库外键。应用端口 `DiscussionTargetResolver` 在创建评论前校验目标存在且公开；组合逻辑只放在顶层 `composition`，业务模块保持互不依赖。

## 3. 积分与权益

内容解锁只使用整数积分 `credit`，页面不直接把人民币金额当作内容价格。积分模型在实施阶段做过一次简化：余额和价格放回它们真正所属的行，只保留需要事实来源的两张表。

- `users.credit_balance`：当前余额。扣减是一条带余额校验的原子 UPDATE，不再需要中转账户表，也不再需要乐观锁；
- `publications.credit_price`：一个内容需要多少积分。价格是发布内容自身的属性，不再存到另一张表再 JOIN 回来；
- `credit_ledger_entries`：只追加的变动事实，带变动后余额、来源与唯一幂等键；
- `content_unlocks`：某个用户是否拥有某个内容的访问权，按 `publication_id` 挂载。

余额放在 `users` 上让扣减变成单条语句：

```sql
UPDATE users SET credit_balance = credit_balance - :cost
WHERE id = :id AND credit_balance >= :cost   -- 0 行即余额不足
```

校验与扣减在同一语句内完成，天然原子，没有 CAS 重试循环。流水使用带符号的 `delta`，`GRANT`、`TOP_UP`、`REFUND` 为正，`SPEND`、`EXPIRE` 为负，由 CHECK 约束保证符号与原因一致。重复业务请求依靠 `idempotency_key` 只执行一次。**不能只修改余额而没有流水**；流水的修复方式是按账本重算，而不是继续增量补偿。

`content_unlocks` 按 `publication_id` 而不是 slug 挂载。早期设计用 `target_key = slug`，而改 slug 的路径只搬运报价、不认识权益，会让已购权益全部失配——用户付过积分却打不开内容。改用内容主键后，slug 从此与权益无关，两张表之间也有了真正的外键。

解锁内容的事务顺序为：

1. 读取当前有效价格（`publications.credit_price`）；
2. 原子扣减余额并校验是否足额；
3. 追加 `SPEND` 流水，记录变动后余额与幂等键；
4. 幂等创建内容解锁记录；
5. 提交事务后返回新余额与解锁状态。

## 4. 支付放在积分之后

支付不是内容购买入口，而是积分充值的一种资金来源。接入支付时再新增 `top_up_orders / payment_attempts / payment_events`：支付渠道回调成功后，以渠道流水号作为幂等来源增加积分。支付退款对应反向积分流水；若积分已消费，不允许简单把账户扣成负数，需要单独的退款和风险处理规则。

因此当前阶段不创建支付订单，也不在内容表保存 `price_minor/currency` 的新用法。先完成积分余额、流水、内容价格和解锁记录，再接支付渠道。

## 5. 登录态

浏览器继续只持有 Sa-Token 设置的同域 `HttpOnly` Cookie，不新增前端 JWT，也不把 token 写入 `localStorage`。服务端登录态使用官方 Redis DAO，与验证码和限流共享 Redis 连接但使用独立 key。PostgreSQL 仍保存用户和业务事实。

Redis 登录态解决服务重启和多实例共享问题。它不等于永久登录：会话仍受总有效期、活跃超时、主动退出和 Redis 数据保留策略约束。

## 6. 实施顺序

1. Redis 会话、评论树 API、积分与权益数据库约束；
2. `Publication/publications` 内容迁移与内容类型（已完成）；
3. 把文章详情、阅读器和讨论页接入真实内容与评论接口；
4. 实现积分账户、管理员发放、积分解锁和我的权益；
5. 接入收藏、点赞、阅读进度；
6. 最后接入积分充值支付。

评论先于支付实施，因为它直接建立社区感，也能验证全局挂载模型；支付留到积分账本和幂等规则稳定后再做。
