package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.PublicationReactionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/** 单表关系 CRUD。 */
@Mapper
public interface PublicationReactionMapper extends BaseMapper<PublicationReactionPO> {}
