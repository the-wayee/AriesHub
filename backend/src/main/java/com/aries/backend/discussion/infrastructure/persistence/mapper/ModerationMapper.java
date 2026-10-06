package com.aries.backend.discussion.infrastructure.persistence.mapper;

import com.aries.backend.discussion.application.port.ModerationReadPort.Comment;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ModerationMapper {
    List<Comment> list(
            @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

    long count(@Param("status") String status);
}
