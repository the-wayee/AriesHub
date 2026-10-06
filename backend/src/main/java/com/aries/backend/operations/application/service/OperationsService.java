package com.aries.backend.operations.application.service;

import com.aries.backend.operations.application.port.OperationsReadPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 运营只读统计；一致快照读取实际记录，不承担余额变更或支付规则。 */
@Service
@RequiredArgsConstructor
@Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class OperationsService {
    private final OperationsReadPort reads;

    public record Overview(OperationsReadPort.Summary summary, List<OperationsReadPort.Day> days) {}

    public record LedgerPage(
            List<OperationsReadPort.Ledger> items, int page, int size, long total) {}

    public Overview overview() {
        return new Overview(reads.summary(), reads.days());
    }

    public LedgerPage ledger(int page, int size) {
        return new LedgerPage(
                reads.ledger(size, (page - 1) * size), page, size, reads.ledgerCount());
    }
}
