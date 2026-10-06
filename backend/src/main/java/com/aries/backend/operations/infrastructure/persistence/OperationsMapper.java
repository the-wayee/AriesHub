package com.aries.backend.operations.infrastructure.persistence;

import com.aries.backend.operations.application.port.OperationsReadPort;
import com.aries.backend.operations.infrastructure.persistence.po.LedgerEntryPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 单表计数复用 BaseMapper；跨表汇总与带成员资料的流水投影集中在 XML。 */
@Mapper
public interface OperationsMapper extends BaseMapper<LedgerEntryPO> {
    OperationsReadPort.Summary summary();

    List<OperationsReadPort.Day> days();

    List<OperationsReadPort.Ledger> ledger(@Param("limit") int limit, @Param("offset") int offset);
}
