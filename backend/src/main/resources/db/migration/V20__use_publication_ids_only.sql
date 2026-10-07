-- 开发数据一次性整理：文章标识统一为 ID，运行时代码不再保留名称地址回退。
-- 若同一文章已经同时存在名称线程和 ID 线程，合并评论但保留更严格的审核状态。
CREATE TEMP TABLE publication_thread_merges ON COMMIT DROP AS
SELECT old.id AS old_id, canonical.id AS canonical_id
FROM discussion_threads old
JOIN publications p ON p.slug = old.target_key
JOIN discussion_threads canonical ON canonical.target_type = 'PUBLICATION'
    AND canonical.target_key = p.id::text AND canonical.is_deleted = false
WHERE old.target_type = 'PUBLICATION' AND old.is_deleted = false
    AND old.id <> canonical.id;

UPDATE discussion_threads canonical
SET status = CASE WHEN canonical.status = 'HIDDEN' OR old.status = 'HIDDEN' THEN 'HIDDEN'
                  WHEN canonical.status = 'LOCKED' OR old.status = 'LOCKED' THEN 'LOCKED'
                  ELSE 'OPEN' END,
    updated_at = now()
FROM publication_thread_merges m JOIN discussion_threads old ON old.id = m.old_id
WHERE canonical.id = m.canonical_id;

-- 评论 ID、父回复关系及点赞关联均不变，只统一所属线程。
UPDATE comments c SET thread_id = m.canonical_id
FROM publication_thread_merges m WHERE c.thread_id = m.old_id;
UPDATE discussion_threads t SET is_deleted = true, updated_at = now()
FROM publication_thread_merges m WHERE t.id = m.old_id;

UPDATE discussion_threads t SET target_key = p.id::text, updated_at = now()
FROM publications p WHERE t.target_type = 'PUBLICATION' AND t.target_key = p.slug;

-- 所有运行时投影、仓储和接口均已移除该字段；分类自己的 slug 不受影响。
ALTER TABLE publications DROP COLUMN slug;
