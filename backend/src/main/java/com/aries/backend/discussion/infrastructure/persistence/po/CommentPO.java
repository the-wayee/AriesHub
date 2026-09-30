package com.aries.backend.discussion.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("comments")
public class CommentPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long threadId;
    private Long authorId;
    private Long parentId;
    private Long rootId;
    private Integer depth;
    private String body;
    private String status;
    private OffsetDateTime editedAt;
}
