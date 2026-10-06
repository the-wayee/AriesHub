package com.aries.backend.discussion.infrastructure.persistence;

import com.aries.backend.discussion.application.port.ModerationReadPort;
import com.aries.backend.discussion.infrastructure.persistence.mapper.CommentMapper;
import com.aries.backend.discussion.infrastructure.persistence.mapper.ModerationMapper;
import com.aries.backend.discussion.infrastructure.persistence.po.CommentPO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

/** 审核列表保留联表投影；总数使用评论 BaseMapper，复用逻辑删除过滤。 */
@Repository
@RequiredArgsConstructor
public class MybatisModerationReadPort implements ModerationReadPort {
    private final ModerationMapper mapper;
    private final CommentMapper comments;

    public List<Comment> list(String status, int limit, int offset) {
        return mapper.list(status, limit, offset);
    }

    public long count(String status) {
        return comments.selectCount(
                Wrappers.<CommentPO>lambdaQuery()
                        .eq(!status.isEmpty(), CommentPO::getStatus, status));
    }
}
