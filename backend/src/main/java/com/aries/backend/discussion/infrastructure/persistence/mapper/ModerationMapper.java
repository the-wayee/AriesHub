package com.aries.backend.discussion.infrastructure.persistence.mapper;

import com.aries.backend.discussion.application.port.ModerationReadPort.Comment;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** 审核联表只读投影，正文删除状态的脱敏在 XML 中执行。 */
@Mapper
public interface ModerationMapper {
    List<Comment> list(
            @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);
}
