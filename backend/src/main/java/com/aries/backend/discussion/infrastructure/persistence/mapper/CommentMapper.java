package com.aries.backend.discussion.infrastructure.persistence.mapper;

import com.aries.backend.discussion.infrastructure.persistence.po.CommentPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 评论 Mapper。
 *
 * <p>读取以 {@code BaseMapper} + Lambda Wrapper 与只读投影 XML 为主，
 * 这里只保留 Wrapper 表达不了的两条写入语句。
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentPO> {

    /**
     * 点赞计数的原子加减。
     *
     * <p>这是必须手写的一条：{@code like_count = like_count + delta} 是列对列的自引用表达式，
     * Wrapper 的 {@code set()} 只能赋常量。若改成「先读后写」再 {@code set()}，
     * 两个并发点赞会各自读到同一个旧值并互相覆盖，丢掉一次计数。
     *
     * <p>{@code delta} 由服务端根据插入是否成功决定（只有 +1 / -1），不是用户输入，仍以绑定参数传入。
     * {@code GREATEST} 兜住下界，避免计数异常时触发 CHECK 约束让一次点击报错。
     */
    @Update("""
            UPDATE comments
            SET like_count = GREATEST(0, like_count + #{delta}), updated_at = now()
            WHERE id = #{commentId}
            """)
    int adjustLikeCount(@Param("commentId") long commentId, @Param("delta") int delta);

    /**
     * 幂等插入点赞：同一用户对同一评论并发点赞时只有一条能成功。
     *
     * <p>用 {@code ON CONFLICT DO NOTHING} 而不是「先查再插」：后者在并发下会插入两条，
     * 由唯一索引抛异常，而异常会让整个事务进入失败状态。
     */
    @Insert("""
            INSERT INTO comment_likes(comment_id, user_id, created_at)
            VALUES (#{commentId}, #{userId}, now())
            ON CONFLICT (comment_id, user_id) DO NOTHING
            """)
    int insertLikeIfAbsent(@Param("commentId") long commentId, @Param("userId") long userId);
}
