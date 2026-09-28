-- 仅在显式 dev 配置下加载；生产数据库严禁启用此迁移目录。
INSERT INTO categories (slug, name, sort_order) VALUES
('ai-ppt', 'AI 演示', 10), ('coding', 'AI 编程', 20), ('automation', '日常自动化', 30)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO cases (category_id, slug, title, summary, access_type, price_minor, status, is_demo, published_at)
SELECT id, 'ai-ppt-outline', '把一个主题，整理成一份演示提纲',
'从听众、结论和证据出发，练习用 AI 构建清晰的 PPT 内容结构。', 'FREE', 0, 'PUBLISHED', true, '2026-09-28T09:00:00Z'
FROM categories WHERE slug = 'ai-ppt' ON CONFLICT (slug) DO NOTHING;
INSERT INTO cases (category_id, slug, title, summary, access_type, price_minor, status, is_demo, published_at)
SELECT id, 'codex-focus-page', '用 Codex 做一个专注计时页面',
'把一个小需求拆成界面、交互和验收步骤，了解从想法到网页的过程。', 'PAID', 1990, 'PUBLISHED', true, '2026-09-27T09:00:00Z'
FROM categories WHERE slug = 'coding' ON CONFLICT (slug) DO NOTHING;
INSERT INTO cases (category_id, slug, title, summary, access_type, price_minor, status, is_demo, published_at)
SELECT id, 'organize-materials', '给散落的素材，建立一套整理规则',
'先定义命名和分类规则，再用小批量样本验证，让素材更容易找到。', 'FREE', 0, 'PUBLISHED', true, '2026-09-26T09:00:00Z'
FROM categories WHERE slug = 'automation' ON CONFLICT (slug) DO NOTHING;

INSERT INTO case_contents (case_id, preview_markdown, full_markdown, requirements, deliverables)
SELECT id, $$## 从一份清晰的任务说明开始

先回答三个问题：这份演示给谁看？希望对方记住什么？有哪些事实能支撑结论？

这是用于体验阅读流程的演示案例，不包含可下载模板。$$,
$$## 01 / 写出任务说明

例如：为团队制作一份 5 页的项目周报，读者是同事，目标是让大家了解进度、风险和下周计划。

## 02 / 请 AI 先整理结构

可以提供这样的指令：

> 请根据下面的真实材料整理 5 页演示提纲。每页包含一个结论标题、三条要点和需要补充的证据。缺少的信息请标注待补充，不要编造数据。

## 03 / 检查再制作

- 每页是否只有一个主要结论？
- 数据和引用能否回到原始材料核实？
- 页与页之间是否存在重复或跳跃？

先修正提纲，再进入版式制作。这个练习的成果是一份内容结构，不是自动生成的成品 PPT。

## 使用说明

本页面为演示教程，没有附件或收益承诺。请使用自己的资料，不要向外部工具上传保密信息。$$,
'能整理一段自己的项目材料；可使用任意具备文字对话能力的 AI 工具。工具费用需自行确认。',
'本页文字教程与示例指令，不包含 PPT 文件或人工代做。'
FROM cases WHERE slug = 'ai-ppt-outline' ON CONFLICT (case_id) DO NOTHING;

INSERT INTO case_contents (case_id, preview_markdown, full_markdown, requirements, deliverables)
SELECT id, $$## 一次只解决一个小问题

把需求写成可验证的行为：设置时长、开始计时、暂停和重新开始。再让 AI 分步实现。

当前是付费预览演示，暂未开放购买，也没有可交付源码。$$,
'PRIVATE_DEMO_BODY: 这是用于验证公开接口不会泄露付费正文的占位内容，不能作为商品出售。',
'了解浏览器基本操作；后续正式案例会列明开发工具与运行条件。',
'计划包含源码和操作步骤；当前仅展示商品预览，不售卖、不提供下载。'
FROM cases WHERE slug = 'codex-focus-page' ON CONFLICT (case_id) DO NOTHING;

INSERT INTO case_contents (case_id, preview_markdown, full_markdown, requirements, deliverables)
SELECT id, $$## 先定规则，再做自动化

面对散落的图片、文稿和视频，先用十份素材试验分类方法，再考虑批量处理。$$,
$$## 01 / 建立一个小样本

挑选十份不含敏感信息的素材，记录现有名称、用途和所属项目。

## 02 / 统一命名

例如采用 `日期_项目_主题_版本` 的结构。不要直接覆盖原文件，先把新旧名称写成对照表。

## 03 / 验证可找回性

- 能否根据项目快速找到文件？
- 文件名相同是否会发生覆盖？
- 新命名是否保留了正确的扩展名？

## 04 / 再交给工具

把确认过的规则提供给 AI，让它先输出改名计划。检查无误并备份后，再考虑执行脚本。

这是方法演示，不包含可执行脚本或附件。$$,
'准备十份可公开或可自行处理的样本文件；不要使用唯一副本进行实验。',
'命名规则、核对清单和方法说明，不包含批量操作服务。'
FROM cases WHERE slug = 'organize-materials' ON CONFLICT (case_id) DO NOTHING;
