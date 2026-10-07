package com.aries.backend.catalog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 文章与素材的绑定；数据库以 publication_id、file_id 联合唯一。 */
@Data
@TableName("publication_media_bindings")
public class PublicationMediaBindingPO {
    @TableId(type = IdType.INPUT)
    private Long publicationId;

    private String fileId;
    private Boolean publiclyVisible;
    private Boolean resourceAttachment;
}
