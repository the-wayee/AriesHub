package com.aries.backend.discussion.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("discussion_threads")
public class DiscussionThreadPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private String targetKey;
    private String status;
}
