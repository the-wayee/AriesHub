package com.aries.backend.discussion.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 评论点赞行。
 *
 * <p>刻意<b>不</b>继承 {@link com.aries.backend.shared.infrastructure.persistence.po.BasePO}：
 * 点赞没有「逻辑删除」语义，取消点赞是物理删除。若继承 BasePO，{@code is_deleted} 会带上
 * {@code @TableLogic}，取消点赞变成软删，重新点赞就得先把历史行复活，
 * 唯一约束也必须退化成只覆盖有效行的偏索引——正是要避免的复杂度。
 */
@Getter
@Setter
@TableName("comment_likes")
public class CommentLikePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long userId;
    private OffsetDateTime createdAt;
}
