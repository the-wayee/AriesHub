-- 点赞改为物理删除的普通行。
--
-- 原设计把 comment_likes 当成可逻辑删除的表（is_deleted + 只覆盖有效行的偏唯一索引），
-- 于是「取消点赞」是软删、「重新点赞」就得把历史行 UPDATE 复活，围绕这条复活路径写了一批手写 SQL。
-- 点赞没有留痕价值：取消就是取消，不需要一条历史行，也不需要 is_deleted 参与去重。
--
-- 改成物理删除后，唯一约束变成普通的 (comment_id, user_id)，
-- 「取消再点赞」回到一条普通 INSERT ... ON CONFLICT DO NOTHING，代码里少一整个分支。

-- 先清掉可能残留的逻辑删除历史行，否则新唯一约束会因重复对而失败。
DELETE FROM comment_likes WHERE is_deleted = true;

DROP INDEX comment_likes_comment_user_active_idx;

ALTER TABLE comment_likes DROP COLUMN is_deleted;

CREATE UNIQUE INDEX comment_likes_comment_user_unique_idx
    ON comment_likes (comment_id, user_id);

COMMENT ON TABLE comment_likes IS '评论点赞；取消点赞即物理删除，不做逻辑删除';
