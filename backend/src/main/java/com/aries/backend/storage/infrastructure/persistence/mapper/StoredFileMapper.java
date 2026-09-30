package com.aries.backend.storage.infrastructure.persistence.mapper;

import com.aries.backend.storage.infrastructure.persistence.po.StoredFilePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StoredFileMapper extends BaseMapper<StoredFilePO> {}
