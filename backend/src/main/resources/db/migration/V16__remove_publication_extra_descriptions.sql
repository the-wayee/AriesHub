-- 文章说明统一写入正文；移除不再使用的独立前置条件与交付说明。
ALTER TABLE publication_contents
    DROP COLUMN requirements,
    DROP COLUMN deliverables;
