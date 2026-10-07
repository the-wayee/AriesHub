-- 仅 dev 加载。演示身份禁止登录；事件键和评论标记保证重复执行不重复造数据。
-- 排在 demo_catalog 后，目标均为真实文章/评论 ID，生产配置不加载本目录。
DO $$
DECLARE
    actor_ids bigint[] := ARRAY[]::bigint[];
    actor bigint;
    thread bigint;
    root_comment bigint;
    reply_comment bigint;
    event_kind text;
    subject_kind text;
    subject_reference text;
    event_kinds text[] := ARRAY['MEMBER_JOINED','PUBLICATION_PUBLISHED',
        'DISCUSSION_COMMENTED','PUBLICATION_LIKE','PUBLICATION_BOOKMARK',
        'DISCUSSION_REPLIED','PUBLICATION_SHARE','DISCUSSION_LIKED'];
    names text[] := ARRAY['林小雨','周以宁','宋言','陈予安'];
    i integer;
    table_name text;
    largest_id bigint;
BEGIN
    -- 本地历史脚本曾显式指定 ID；先校准序列，保留已有数据和已分配序列值。
    -- 启动迁移中持锁，避免校准期间业务插入拿到旧序列值。
    LOCK TABLE users, discussion_threads, comments, comment_likes, community_events IN SHARE ROW EXCLUSIVE MODE;
    FOREACH table_name IN ARRAY ARRAY['users','discussion_threads','comments','comment_likes','community_events'] LOOP
        EXECUTE format('SELECT COALESCE(max(id),1) FROM %I',table_name) INTO largest_id;
        PERFORM setval(pg_get_serial_sequence(table_name,'id'),
            GREATEST(largest_id,COALESCE(pg_sequence_last_value(pg_get_serial_sequence(table_name,'id')::regclass),1)),true);
    END LOOP;
    FOR i IN 1..4 LOOP
        INSERT INTO users(email,password_hash,nickname,status)
        VALUES ('activity-' || i || '@demo.arieshub.invalid',
            '$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW',names[i],'DISABLED')
        ON CONFLICT (email) WHERE is_deleted = false DO NOTHING;
        SELECT id INTO actor FROM users
        WHERE email = 'activity-' || i || '@demo.arieshub.invalid' AND is_deleted = false;
        actor_ids := array_append(actor_ids,actor);
    END LOOP;
    INSERT INTO discussion_threads(target_type,target_key)
    SELECT 'PUBLICATION',id::text FROM publications WHERE id = 1 AND NOT is_deleted
    ON CONFLICT (target_type,target_key) WHERE is_deleted = false DO NOTHING;
    SELECT id INTO thread FROM discussion_threads
    WHERE target_type = 'PUBLICATION' AND target_key = '1' AND NOT is_deleted;
    IF thread IS NULL THEN RETURN; END IF;
    SELECT id INTO root_comment FROM comments WHERE thread_id = thread
        AND author_id = actor_ids[1] AND body = '提纲先写清楚，再交给 AI 补充细节，这个顺序很实用。';
    IF root_comment IS NULL THEN
        INSERT INTO comments(thread_id,author_id,body)
        VALUES(thread,actor_ids[1],'提纲先写清楚，再交给 AI 补充细节，这个顺序很实用。')
        RETURNING id INTO root_comment;
    END IF;
    SELECT id INTO reply_comment FROM comments WHERE parent_id = root_comment
        AND author_id = actor_ids[2] AND body = '我也试了一次，先列听众的问题，演示结构就清楚了。';
    IF reply_comment IS NULL THEN
        INSERT INTO comments(thread_id,author_id,parent_id,root_id,depth,body)
        VALUES(thread,actor_ids[2],root_comment,root_comment,1,
            '我也试了一次，先列听众的问题，演示结构就清楚了。')
        RETURNING id INTO reply_comment;
    END IF;
    INSERT INTO comment_likes(comment_id,user_id) VALUES(root_comment,actor_ids[3])
    ON CONFLICT(comment_id,user_id) DO NOTHING;
    UPDATE comments SET like_count = (SELECT count(*) FROM comment_likes WHERE comment_id = root_comment)
    WHERE id = root_comment;
    -- 两轮不同身份和文章目标，验证三行轮播、类型筛选和历史分页。
    FOR i IN 1..16 LOOP
        actor := actor_ids[1 + (i - 1) % 4];
        event_kind := event_kinds[1 + (i - 1) % 8];
        subject_kind := 'PUBLICATION'; subject_reference := (1 + (i - 1) % 3)::text;
        IF event_kind = 'MEMBER_JOINED' THEN
            subject_kind := 'USER'; subject_reference := actor::text;
        ELSIF event_kind IN ('DISCUSSION_COMMENTED','DISCUSSION_REPLIED','DISCUSSION_LIKED') THEN
            subject_kind := 'COMMENT';
            subject_reference := CASE WHEN event_kind = 'DISCUSSION_REPLIED'
                THEN reply_comment::text ELSE root_comment::text END;
            IF event_kind = 'DISCUSSION_COMMENTED' THEN actor := actor_ids[1];
            ELSIF event_kind = 'DISCUSSION_REPLIED' THEN actor := actor_ids[2];
            ELSE actor := actor_ids[3]; END IF;
        END IF;
        INSERT INTO community_events(event_key,actor_id,kind,subject_type,subject_id,created_at)
        VALUES('demo-community:' || i,actor,event_kind,subject_kind,subject_reference,
            now() - (17-i) * interval '3 minutes')
        ON CONFLICT(event_key) DO NOTHING;
    END LOOP;
END $$;
