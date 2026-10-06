package com.aries.backend.catalog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 内容领域素材元数据；文件存取仍由通用存储服务提供。 */
@Data
@TableName("publication_assets")
public class PublicationAssetPO {
    @TableId(value = "file_id", type = IdType.INPUT)
    private String id;

    private Long ownerId;
    private String kind;
    private String filename;
    private String contentType;
    private Long size;
}
