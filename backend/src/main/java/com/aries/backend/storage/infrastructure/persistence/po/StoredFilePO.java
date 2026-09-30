package com.aries.backend.storage.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("stored_files")
public class StoredFilePO extends BasePO {
    @TableId(type = IdType.INPUT) private String id;
    private Long ownerId;
    private String purpose;
    private String objectKey;
    private String filename;
    private String contentType;
    private Long size;
}
