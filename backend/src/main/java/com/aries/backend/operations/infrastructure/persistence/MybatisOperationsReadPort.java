package com.aries.backend.operations.infrastructure.persistence;

import com.aries.backend.operations.application.port.OperationsReadPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

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
        return mapper.ledgerCount();
    }
}
