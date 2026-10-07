package com.aries.backend.catalog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.OffsetDateTime;

/** 不可枚举的单文章授权链接；读取时重新验证文章状态，不扩展成登录会话。 */
@Data
@TableName("publication_shares")
public class PublicationSharePO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publicationId;
    private Long userId;
    private String token;
    private OffsetDateTime createdAt;
}
