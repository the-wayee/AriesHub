package com.aries.backend.catalog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.OffsetDateTime;

/** 仅保存已授权免费正文的位置与版本，不保存正文或用 localStorage 代替业务事实。 */
@Data
@TableName("publication_reading_progress")
public class PublicationReadingPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long publicationId;
    private String version;
    private String position;
    private Integer percent;
    private OffsetDateTime updatedAt;
}
