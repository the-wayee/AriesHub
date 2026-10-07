package com.aries.backend.catalog.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/** 只用于既有文章解锁权益校验，不推导余额或创建付费权益。 */
@Getter
@Setter
@TableName("content_unlocks")
public class PublicationUnlockPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long publicationId;
}
