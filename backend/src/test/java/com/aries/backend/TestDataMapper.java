package com.aries.backend;

import org.apache.ibatis.annotations.*;

/** 集成测试数据夹具也通过 MyBatis 执行，避免测试绕过项目的数据访问约定。 */
@Mapper
interface TestDataMapper {
    @Update("""
            TRUNCATE content_entitlements, credit_offers, credit_ledger_entries, credit_accounts,
                comments, discussion_threads, publication_contents, publications, categories, users
            RESTART IDENTITY CASCADE
            """)
    void reset();

    @Insert("INSERT INTO categories(id, slug, name) VALUES (1, 'coding', 'AI 编程'), (2, 'slides', 'AI 演示')")
    void insertCategories();

    @Insert("""
            INSERT INTO publications(id, category_id, slug, title, summary, publication_type,
                access_type, status, delivery_status, published_at)
            VALUES (#{id}, #{categoryId}, #{slug}, #{title}, '内容摘要', 'CASE_STUDY',
                #{accessType}, #{status}, #{deliveryStatus}, '2026-09-28T00:00:00Z')
            """)
    void insertPublication(@Param("id") long id, @Param("categoryId") long categoryId,
                           @Param("slug") String slug, @Param("title") String title,
                           @Param("accessType") String accessType, @Param("status") String status,
                           @Param("deliveryStatus") String deliveryStatus);

    @Insert("""
            INSERT INTO credit_offers(target_type, target_key, credit_price, status)
            VALUES ('PUBLICATION', #{targetKey}, #{creditPrice}, 'ACTIVE')
            """)
    void insertCreditOffer(@Param("targetKey") String targetKey, @Param("creditPrice") long creditPrice);

    @Insert("""
            INSERT INTO publication_contents(publication_id, preview_markdown, full_markdown,
                requirements, deliverables)
            VALUES (#{publicationId}, '公开预览', #{fullMarkdown}, '基础要求', '交付清单')
            """)
    void insertPublicationContent(@Param("publicationId") long publicationId,
                                  @Param("fullMarkdown") String fullMarkdown);

    @Select("SELECT count(*) FROM flyway_schema_history WHERE success")
    int successfulFlywayMigrations();

    @Select("SELECT password_hash FROM users WHERE email = #{email}")
    String passwordHash(@Param("email") String email);

    @Select("SELECT created_at IS NOT NULL AND updated_at IS NOT NULL AND NOT is_deleted FROM users WHERE id = #{id}")
    boolean userAuditFieldsPresent(@Param("id") long id);

    @Select("SELECT last_login_at IS NOT NULL FROM users WHERE email = #{email}")
    boolean userHasLastLogin(@Param("email") String email);

    @Select("SELECT is_deleted FROM users WHERE id = #{id}")
    boolean userDeleted(@Param("id") long id);

    @Update("UPDATE publications SET status = #{status} WHERE id = #{id}")
    void updatePublicationStatus(@Param("id") long id, @Param("status") String status);

    @Update("UPDATE credit_offers SET credit_price = #{creditPrice} WHERE target_key = #{targetKey}")
    void updateCreditOfferPrice(@Param("targetKey") String targetKey, @Param("creditPrice") long creditPrice);

    @Update("UPDATE publications SET access_type = #{accessType} WHERE id = #{id}")
    void updatePublicationAccessType(@Param("id") long id, @Param("accessType") String accessType);

    @Delete("DELETE FROM publication_contents WHERE publication_id = #{publicationId}")
    void deletePublicationContent(@Param("publicationId") long publicationId);

    @Select("SELECT status FROM publications WHERE id = #{id}")
    String publicationStatus(@Param("id") long id);

    @Select("SELECT id FROM publications WHERE slug = #{slug}")
    Long publicationIdBySlug(@Param("slug") String slug);

    @Select("SELECT id FROM comments WHERE body = #{body}")
    Long commentIdByBody(@Param("body") String body);

    @Insert("""
            INSERT INTO users(email, password_hash, nickname, email_verified)
            VALUES ('credit@example.com', 'hash', '积分用户', true)
            """)
    void insertCreditUser();

    @Insert("INSERT INTO credit_accounts(user_id, balance) VALUES (1, 100)")
    void insertCreditAccount();

    @Insert("""
            INSERT INTO credit_ledger_entries(account_id, entry_type, amount, balance_after,
                reference_type, reference_key, idempotency_key)
            VALUES (1, 'GRANT', 100, 100, 'ADMIN_GRANT', 'welcome', 'grant:welcome:1')
            """)
    void insertCreditLedger();

    @Insert("""
            INSERT INTO credit_ledger_entries(account_id, entry_type, amount, balance_after,
                reference_type, reference_key, idempotency_key)
            VALUES (1, 'GRANT', 100, 200, 'ADMIN_GRANT', 'welcome', 'grant:welcome:1')
            """)
    void insertDuplicateCreditLedger();

    @Update("UPDATE credit_accounts SET balance = #{balance} WHERE id = 1")
    void updateCreditBalance(@Param("balance") long balance);
}
