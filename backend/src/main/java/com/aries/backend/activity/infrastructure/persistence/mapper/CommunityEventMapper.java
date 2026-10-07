package com.aries.backend.activity.infrastructure.persistence.mapper;

import com.aries.backend.activity.infrastructure.persistence.po.CommunityEventPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityEventMapper extends BaseMapper<CommunityEventPO> {
    /** 唯一键与 ON CONFLICT 原子去重，避免并发重投让整个来源事务失败。 */
    @Insert(
            "INSERT INTO"
                + " community_events(event_key,actor_id,kind,subject_type,subject_id,created_at)"
                + " VALUES(#{event.eventKey},#{event.actorId},#{event.kind},#{event.subjectType},#{event.subjectId},#{event.createdAt})"
                + " ON CONFLICT(event_key) DO NOTHING")
    int appendOnce(@Param("event") CommunityEventPO event);
}
