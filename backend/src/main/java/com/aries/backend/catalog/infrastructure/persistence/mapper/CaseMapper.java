package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.infrastructure.persistence.po.CasePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 简单单表操作使用 MyBatis-Plus；不在 Controller 中暴露通用 CRUD。 */
@Mapper
public interface CaseMapper extends BaseMapper<CasePO> {}
