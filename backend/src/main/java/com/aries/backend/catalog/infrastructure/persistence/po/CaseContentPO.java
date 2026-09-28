package com.aries.backend.catalog.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 案例正文持久化对象，主键同时是案例外键。 */
@Getter
@Setter
@TableName("case_contents")
public class CaseContentPO extends BasePO {
    @TableId(value = "case_id", type = IdType.INPUT)
    private Long caseId;
    private String previewMarkdown;
    private String fullMarkdown;
    private String requirements;
    private String deliverables;
    private String version;
}
