package com.aries.backend.operations.infrastructure.persistence;

import com.aries.backend.operations.application.port.OperationsReadPort;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OperationsMapper {
    OperationsReadPort.Summary summary();

    List<OperationsReadPort.Day> days();

    @Select(
            "SELECT l.id::text AS"
                + " id,u.email,u.nickname,l.delta,l.balance_after,l.reason,l.note,l.created_at FROM"
                + " credit_ledger_entries l JOIN users u ON u.id=l.user_id ORDER BY l.created_at"
                + " DESC,l.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<OperationsReadPort.Ledger> ledger(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT count(*) FROM credit_ledger_entries")
    long ledgerCount();
}
