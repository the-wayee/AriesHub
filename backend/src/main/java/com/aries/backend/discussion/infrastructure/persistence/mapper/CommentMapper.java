package com.aries.backend.discussion.infrastructure.persistence.mapper;

import com.aries.backend.discussion.infrastructure.persistence.po.CommentPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<CommentPO> {}
