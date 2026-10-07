package com.aries.backend.catalog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.OffsetDateTime;

/** 文章收藏/点赞关系行；唯一约束与事务锁共同保证重复操作幂等。 */
@Data
@TableName("publication_reactions")
public class PublicationReactionPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long publicationId;
    private String kind;
    private OffsetDateTime createdAt;
}
