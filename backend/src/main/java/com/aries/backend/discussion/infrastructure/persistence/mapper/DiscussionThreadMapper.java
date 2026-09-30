package com.aries.backend.discussion.infrastructure.persistence.mapper;

import com.aries.backend.discussion.infrastructure.persistence.po.DiscussionThreadPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DiscussionThreadMapper extends BaseMapper<DiscussionThreadPO> {
    @Insert("""
            INSERT INTO discussion_threads(target_type, target_key)
            VALUES (#{targetType}, #{targetKey})
            ON CONFLICT (target_type, target_key) WHERE is_deleted = false DO NOTHING
            """)
    int insertIfAbsent(@Param("targetType") String targetType, @Param("targetKey") String targetKey);
}
