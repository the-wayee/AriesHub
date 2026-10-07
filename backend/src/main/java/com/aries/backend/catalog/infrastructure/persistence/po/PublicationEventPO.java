package com.aries.backend.catalog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.OffsetDateTime;

/** 事件的去重键由服务端生成；匿名读者标识不进入公开动态。 */
@Data
@TableName("publication_events")
public class PublicationEventPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publicationId;
    private Long userId;
    private String kind;
    private String dedupKey;
    private OffsetDateTime createdAt;
}
