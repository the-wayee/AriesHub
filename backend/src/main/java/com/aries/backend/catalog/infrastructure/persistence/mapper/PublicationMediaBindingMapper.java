package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.PublicationMediaBindingPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/** 文章素材绑定单表 Mapper；仓储始终按联合业务键操作。 */
@Mapper
public interface PublicationMediaBindingMapper extends BaseMapper<PublicationMediaBindingPO> {}
