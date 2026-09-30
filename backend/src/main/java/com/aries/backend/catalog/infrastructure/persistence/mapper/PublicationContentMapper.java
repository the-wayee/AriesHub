package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.PublicationContentPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 案例正文单表写入 Mapper。 */
@Mapper
public interface PublicationContentMapper extends BaseMapper<PublicationContentPO> {}
