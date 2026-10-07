package com.aries.backend;

import org.apache.ibatis.annotations.*;

import java.time.OffsetDateTime;

/** 集成测试数据夹具也通过 MyBatis 执行，避免测试绕过项目的数据访问约定。 */
@Mapper
interface TestDataMapper {
    @Update(
            """
            TRUNCATE comment_likes, content_unlocks, credit_ledger_entries,
                comments, discussion_threads, publication_contents, publications, categories, users
            RESTART IDENTITY CASCADE
            """)
    void reset();

    @Select("SELECT count(*) FROM stored_files")
    long storedFileCount();

    @Insert("INSERT INTO categories(slug, name) VALUES ('coding', 'AI 编程'), ('slides', 'AI 演示')")
    void insertCategories();

    @Insert(
            """
            INSERT INTO publications(id, category_id, slug, title, summary, publication_type,
                access_type, credit_price, status, delivery_status, published_at)
            VALUES (#{id}, #{categoryId}, #{slug}, #{title}, '内容摘要', 'CASE_STUDY',
                #{accessType}, #{creditPrice}, #{status}, #{deliveryStatus}, '2026-09-28T00:00:00Z')
            """)
    void insertPublication(
            @Param("id") long id,
            @Param("categoryId") long categoryId,
            @Param("slug") String slug,
            @Param("title") String title,
            @Param("accessType") String accessType,
            @Param("creditPrice") long creditPrice,
            @Param("status") String status,
            @Param("deliveryStatus") String deliveryStatus);

    @Insert(
            """
            INSERT INTO publication_contents(publication_id, preview_markdown, full_markdown,
                requirements, deliverables)
            VALUES (#{publicationId}, '公开预览', #{fullMarkdown}, '基础要求', '交付清单')
            """)
    void insertPublicationContent(
            @Param("publicationId") long publicationId, @Param("fullMarkdown") String fullMarkdown);

    @Select("SELECT count(*) FROM flyway_schema_history WHERE success")
    int successfulFlywayMigrations();

    @Select("SELECT password_hash FROM users WHERE email = #{email}")
    String passwordHash(@Param("email") String email);

    @Select(
            "SELECT created_at IS NOT NULL AND updated_at IS NOT NULL AND NOT is_deleted FROM users"
                + " WHERE id = #{id}")
    boolean userAuditFieldsPresent(@Param("id") long id);

    @Select("SELECT last_login_at IS NOT NULL FROM users WHERE email = #{email}")
    boolean userHasLastLogin(@Param("email") String email);

    @Select("SELECT is_deleted FROM users WHERE id = #{id}")
    boolean userDeleted(@Param("id") long id);

    @Update("UPDATE publications SET status = #{status} WHERE id = #{id}")
    void updatePublicationStatus(@Param("id") long id, @Param("status") String status);

    @Update("UPDATE publications SET credit_price = #{creditPrice} WHERE slug = #{slug}")
    void updatePublicationCreditPrice(
            @Param("slug") String slug, @Param("creditPrice") long creditPrice);

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

    @Select("SELECT id FROM users WHERE email = #{email}")
    Long userIdByEmail(@Param("email") String email);

    @Insert(
            """
            INSERT INTO discussion_threads(target_type, target_key, status)
            VALUES (#{targetType}, #{targetKey}, #{status})
            ON CONFLICT (target_type, target_key) WHERE is_deleted = false DO NOTHING
            """)
    void insertThread(
            @Param("targetType") String targetType,
            @Param("targetKey") String targetKey,
            @Param("status") String status);

    @Select(
            """
            SELECT id FROM discussion_threads
            WHERE target_type = #{targetType} AND target_key = #{targetKey} AND is_deleted = false
            """)
    Long threadId(@Param("targetType") String targetType, @Param("targetKey") String targetKey);

    /** 直接插入根评论，用于批量构造分页场景。created_at 显式给出， 否则同一事务内 now() 相同，排序只能靠 id 兜底。 */
    @Insert(
            """
            INSERT INTO comments(thread_id, author_id, depth, body, status, created_at, updated_at)
            VALUES (#{threadId}, #{authorId}, 0, #{body}, 'PUBLISHED',
                    now() - (#{minutesAgo} || ' minutes')::interval, now())
            """)
    void insertRootComment(
            @Param("threadId") long threadId,
            @Param("authorId") long authorId,
            @Param("body") String body,
            @Param("minutesAgo") int minutesAgo);

    /** 直接插入一条回复，挂在指定根评论下。 */
    @Insert(
            """
            INSERT INTO comments(thread_id, author_id, parent_id, root_id, depth, body, status,
                created_at, updated_at)
            VALUES (#{threadId}, #{authorId}, #{rootId}, #{rootId}, 1, #{body}, 'PUBLISHED',
                    now() - (#{minutesAgo} || ' minutes')::interval, now())
            """)
    void insertReply(
            @Param("threadId") long threadId,
            @Param("authorId") long authorId,
            @Param("rootId") long rootId,
            @Param("body") String body,
            @Param("minutesAgo") int minutesAgo);

    @Update("UPDATE comments SET like_count = #{likeCount} WHERE id = #{commentId}")
    void setLikeCount(@Param("commentId") long commentId, @Param("likeCount") int likeCount);

    @Select("SELECT like_count FROM comments WHERE id = #{commentId}")
    int likeCount(@Param("commentId") long commentId);

    @Select("SELECT count(*) FROM comment_likes WHERE comment_id = #{commentId}")
    int activeLikes(@Param("commentId") long commentId);

    @Select(
            """
            SELECT updated_at FROM discussion_threads
            WHERE target_type = #{targetType} AND target_key = #{targetKey} AND is_deleted = false
            """)
    OffsetDateTime threadUpdatedAt(
            @Param("targetType") String targetType, @Param("targetKey") String targetKey);

    @Select("SELECT updated_at FROM comments WHERE id = #{commentId}")
    OffsetDateTime commentUpdatedAt(@Param("commentId") long commentId);

    @Select("SELECT status FROM discussion_threads WHERE id = #{threadId}")
    String threadStatus(@Param("threadId") long threadId);

    @Select("SELECT status FROM comments WHERE id = #{commentId}")
    String commentStatus(@Param("commentId") long commentId);

    @Insert(
            """
            INSERT INTO users(email, password_hash, nickname, email_verified, credit_balance)
            VALUES ('credit@example.com', 'hash', '积分用户', true, 0)
            """)
    void insertCreditUser();

    @Insert(
            """
            INSERT INTO credit_ledger_entries(user_id, delta, balance_after, reason,
                reference_key, idempotency_key)
            VALUES (1, 100, 100, 'GRANT', 'welcome', 'grant:welcome:1')
            """)
    void insertCreditLedger();

    @Insert(
            """
            INSERT INTO credit_ledger_entries(user_id, delta, balance_after, reason,
                reference_key, idempotency_key)
            VALUES (1, 100, 200, 'GRANT', 'welcome', 'grant:welcome:1')
            """)
    void insertDuplicateCreditLedger();

    @Update("UPDATE users SET credit_balance = #{balance} WHERE id = 1")
    void updateCreditBalance(@Param("balance") long balance);
}
