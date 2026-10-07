package com.aries.backend.activity.domain.repository;

import com.aries.backend.activity.domain.model.CommunityEvent;
import com.aries.backend.activity.domain.model.CommunityEventKind;

import java.util.List;

/** 动态事实只追加；目标下架不删除历史，当前可见性由展示用例重新判断。 */
public interface CommunityEventRepository {
    /** 相同事件键只记录一次；与来源业务事务一起提交或回滚。 */
    void append(CommunityEvent event);

    /** 按事件 ID 倒序读取候选；before 为排他游标，null 从最新开始，kinds 为空表示全部。 */
    List<CommunityEvent> recent(int size, Long before, List<CommunityEventKind> kinds);
}
