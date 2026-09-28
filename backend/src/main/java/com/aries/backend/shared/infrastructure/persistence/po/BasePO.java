package com.aries.backend.shared.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 数据库通用审计字段。
 *
 * <p>创建和更新操作由 MyBatis-Plus 自动填充，删除操作使用逻辑删除，避免业务数据被直接物理清除。</p>
 */
@Getter
@Setter
public abstract class BasePO {
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}
