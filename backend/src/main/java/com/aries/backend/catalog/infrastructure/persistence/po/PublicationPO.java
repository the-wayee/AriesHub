package com.aries.backend.catalog.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 数据库行对象，仅在基础设施层使用，不得直接返回给前端。 */
@Getter
@Setter
@TableName("publications")
public class PublicationPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    @com.baomidou.mybatisplus.annotation.TableField(
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private String coverFileId;

    private Boolean featured;
    private String title;
    private String summary;
    private String publicationType;
    private String accessType;
    private Long creditPrice;
    private String status;
    private String deliveryStatus;
    private OffsetDateTime publishedAt;
}
