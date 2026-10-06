package com.aries.backend.discussion.infrastructure.persistence;

import com.aries.backend.discussion.application.port.ModerationReadPort;
import com.aries.backend.discussion.infrastructure.persistence.mapper.ModerationMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MybatisModerationReadPort implements ModerationReadPort {
    private final ModerationMapper mapper;

    public List<Comment> list(String status, int limit, int offset) {
        return mapper.list(status, limit, offset);
    }

    public long count(String status) {
        return mapper.count(status);
    }
}
