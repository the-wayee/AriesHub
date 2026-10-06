package com.aries.backend.operations.application.port;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface OperationsReadPort {
    record Summary(
            long members,
            long activeMembers,
            long newMembers,
            long published,
            long drafts,
            long paid,
            long comments,
            long unlocks,
            long creditsSpent,
            long creditBalance) {}

    record Day(LocalDate date, long registrations, long publications, long comments) {}

    record Ledger(
            String id,
            String email,
            String nickname,
            long delta,
            long balanceAfter,
            String reason,
            String note,
            OffsetDateTime createdAt) {}

    Summary summary();

    List<Day> days();

    List<Ledger> ledger(int limit, int offset);

    long ledgerCount();
}
