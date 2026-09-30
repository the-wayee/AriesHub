package com.aries.backend.catalog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
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
    private String slug;
    private String title;
    private String summary;
    private String publicationType;
    private String accessType;
    private Long creditPrice;
    private String status;
    private String deliveryStatus;
    private OffsetDateTime publishedAt;
}
