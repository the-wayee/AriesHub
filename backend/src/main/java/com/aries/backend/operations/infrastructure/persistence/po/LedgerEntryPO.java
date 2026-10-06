package com.aries.backend.operations.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 运营只读账本投影；账本不支持更新或软删，因此不继承 BasePO。 */
@Getter
@Setter
@TableName("credit_ledger_entries")
public class LedgerEntryPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long delta;
    private Long balanceAfter;
    private String reason;
    private String referenceKey;
    private String idempotencyKey;
    private String note;
    private OffsetDateTime createdAt;
}
