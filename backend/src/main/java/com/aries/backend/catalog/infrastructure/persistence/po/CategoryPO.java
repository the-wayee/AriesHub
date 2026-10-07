package com.aries.backend.catalog.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/** 分类表持久化对象。 */
@Getter
@Setter
@TableName("categories")
public class CategoryPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String slug;
    private String name;
    private String color;
    private Integer sortOrder;
}
