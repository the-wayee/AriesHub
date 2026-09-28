package com.aries.backend.identity.infrastructure.persistence.mapper;

import com.aries.backend.identity.infrastructure.persistence.po.UserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 用户单表读写使用 MyBatis-Plus 提供的基础能力。 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {}
