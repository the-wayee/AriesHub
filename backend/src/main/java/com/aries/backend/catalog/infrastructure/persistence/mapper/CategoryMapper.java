package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.CategoryPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/** 分类单表查询 Mapper。 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryPO> {}
