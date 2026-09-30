-- 把旧的案例目录整体迁移为通用发布内容。V1-V3 保持不可变，现有数据原位升级。
ALTER TABLE cases RENAME TO publications;
ALTER TABLE case_contents RENAME TO publication_contents;
ALTER TABLE publication_contents RENAME COLUMN case_id TO publication_id;

ALTER TABLE publications RENAME CONSTRAINT cases_pkey TO publications_pkey;
ALTER TABLE publications RENAME CONSTRAINT cases_slug_key TO publications_slug_key;
ALTER TABLE publication_contents RENAME CONSTRAINT case_contents_pkey TO publication_contents_pkey;
ALTER TABLE publication_contents RENAME CONSTRAINT case_contents_case_id_fkey
    TO publication_contents_publication_id_fkey;

ALTER INDEX cases_published_idx RENAME TO publications_published_idx;
ALTER INDEX cases_category_idx RENAME TO publications_category_idx;

ALTER TABLE publications
    ADD COLUMN publication_type varchar(20) NOT NULL DEFAULT 'CASE_STUDY'
        CHECK (publication_type IN ('CASE_STUDY', 'ARTICLE', 'COURSE'));

-- 旧人民币分只用于一次性换算迁移；新内容仅使用整数积分报价。
INSERT INTO credit_offers(target_type, target_key, credit_price, status)
SELECT 'PUBLICATION', slug, GREATEST(1, price_minor / 10), 'ACTIVE'
FROM publications
WHERE access_type = 'PAID' AND is_deleted = false
ON CONFLICT (target_type, target_key) WHERE is_deleted = false DO NOTHING;

ALTER TABLE publications
    DROP CONSTRAINT cases_access_type_check,
    DROP CONSTRAINT cases_price_minor_check,
    DROP CONSTRAINT cases_currency_check,
    DROP CONSTRAINT cases_check;

UPDATE publications SET access_type = 'CREDIT' WHERE access_type = 'PAID';

ALTER TABLE publications
    ADD CONSTRAINT publications_access_type_check
        CHECK (access_type IN ('FREE', 'CREDIT')),
    DROP COLUMN price_minor,
    DROP COLUMN currency;

COMMENT ON TABLE publications IS '主理人发布的实战案例、文章和课程';
COMMENT ON COLUMN publications.publication_type IS 'CASE_STUDY、ARTICLE 或 COURSE';
COMMENT ON COLUMN publications.access_type IS 'FREE 或通过 credit_offers 积分解锁';
