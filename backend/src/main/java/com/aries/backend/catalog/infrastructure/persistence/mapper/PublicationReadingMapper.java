package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.PublicationReadingPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/** 单表阅读记录 CRUD。 */
@Mapper
public interface PublicationReadingMapper extends BaseMapper<PublicationReadingPO> {}
