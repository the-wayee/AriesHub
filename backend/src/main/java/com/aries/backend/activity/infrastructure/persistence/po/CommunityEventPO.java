package com.aries.backend.activity.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 不使用逻辑删除：事件是不可变的事实，展示时校验目标当前可见性。 */
@Getter
@Setter
@TableName("community_events")
public class CommunityEventPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventKey;
    private Long actorId;
    private String kind;
    private String subjectType;
    private String subjectId;
    private OffsetDateTime createdAt;
}
