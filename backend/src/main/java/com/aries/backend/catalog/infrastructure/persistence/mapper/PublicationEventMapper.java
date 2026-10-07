package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.PublicationEventPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PublicationEventMapper extends BaseMapper<PublicationEventPO> {}
