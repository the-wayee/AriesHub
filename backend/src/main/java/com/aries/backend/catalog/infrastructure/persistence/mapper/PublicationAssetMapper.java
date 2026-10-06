package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.PublicationAssetPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/** 素材元数据单表 Mapper，查询与写入使用 BaseMapper。 */
@Mapper
public interface PublicationAssetMapper extends BaseMapper<PublicationAssetPO> {}
