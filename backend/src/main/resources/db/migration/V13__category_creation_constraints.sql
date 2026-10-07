-- 同名分类不能重复创建；逻辑删除的分类不占用名称。
CREATE UNIQUE INDEX categories_active_name_unique ON categories(lower(btrim(name))) WHERE is_deleted = false;
-- 兼容历史种子数据显式指定 ID 的情况，后续创建从最大 ID 之后继续。
SELECT setval(pg_get_serial_sequence('categories', 'id'), COALESCE((SELECT MAX(id) FROM categories), 0) + 1, false);
