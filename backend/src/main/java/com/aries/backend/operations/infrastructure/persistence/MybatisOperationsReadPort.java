package com.aries.backend.operations.infrastructure.persistence;

import com.aries.backend.operations.application.port.OperationsReadPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

/** 运营查询适配器；只读取持久化事实，不提供账本变更能力。 */
@Repository
@RequiredArgsConstructor
public class MybatisOperationsReadPort implements OperationsReadPort {
    private final OperationsMapper mapper;

    public Summary summary() {
        return mapper.summary();
    }

    public List<Day> days() {
        return mapper.days();
    }

    public List<Ledger> ledger(int limit, int offset) {
        return mapper.ledger(limit, offset);
    }

    public long ledgerCount() {
        return mapper.selectCount(null);
    }
}
