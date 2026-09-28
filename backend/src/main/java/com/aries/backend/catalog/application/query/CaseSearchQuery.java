package com.aries.backend.catalog.application.query;

import lombok.Value;

/** 应用层查询条件；接口层负责边界校验，基础设施层负责安全绑定 SQL 参数。 */
@Value
public class CaseSearchQuery {
    int page;
    int size;
    String q;
    String category;
    String access;

    public int getOffset() { return (page - 1) * size; }

    /** 以 ! 为转义符，确保用户输入的百分号、下划线按普通字符检索。 */
    public String getKeyword() {
        return "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
