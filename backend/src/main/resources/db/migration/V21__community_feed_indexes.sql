-- Tab 筛选先于游标分页；全部动态使用主键倒序，分类动态复用此索引。
CREATE INDEX community_events_kind_id_idx ON community_events (kind, id DESC);
