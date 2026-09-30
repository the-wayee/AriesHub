-- 演示数据由 dev 专用迁移目录决定，不属于 Publication 的业务状态。
ALTER TABLE publications DROP COLUMN is_demo;

-- 回复可以持续指向任意已发布评论；depth 仅用于树形投影，不再作为业务上限。
ALTER TABLE comments DROP CONSTRAINT comments_depth_check;
ALTER TABLE comments ALTER COLUMN depth TYPE integer;
ALTER TABLE comments ADD CONSTRAINT comments_depth_non_negative_check CHECK (depth >= 0);
