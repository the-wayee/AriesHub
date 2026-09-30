package com.aries.backend.catalog.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("credit_offers")
public class CreditOfferPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private String targetKey;
    private Long creditPrice;
    private String status;
}
