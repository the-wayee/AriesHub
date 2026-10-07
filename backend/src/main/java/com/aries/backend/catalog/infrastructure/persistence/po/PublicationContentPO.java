package com.aries.backend.catalog.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/** 发布内容正文持久化对象，主键同时是发布内容外键。 */
@Getter
@Setter
@TableName("publication_contents")
public class PublicationContentPO extends BasePO {
    @TableId(value = "publication_id", type = IdType.INPUT)
    private Long publicationId;

    private String previewMarkdown;
    private String fullMarkdown;
    private String version;
}
