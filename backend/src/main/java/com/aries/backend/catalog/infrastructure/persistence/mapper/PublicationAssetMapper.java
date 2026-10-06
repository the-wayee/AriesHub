package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.PublicationAssetPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PublicationAssetMapper extends BaseMapper<PublicationAssetPO> {}
