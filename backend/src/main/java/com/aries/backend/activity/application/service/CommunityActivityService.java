package com.aries.backend.activity.application.service;

import com.aries.backend.activity.application.port.ActivityContext;
import com.aries.backend.activity.application.port.ActivityContext.Target;
import com.aries.backend.activity.application.query.CommunityActivityFilter;
import com.aries.backend.activity.application.view.CommunityActivityPage;
import com.aries.backend.activity.application.view.CommunityActivityView;
import com.aries.backend.activity.domain.model.CommunityEvent;
import com.aries.backend.activity.domain.model.CommunityEventKind;
import com.aries.backend.activity.domain.model.CommunitySubjectType;
import com.aries.backend.activity.domain.repository.CommunityEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 社区动态用例：事实写入与来源事务一致，展示时重新校验身份和目标可见性。 */
@Service
@RequiredArgsConstructor
public class CommunityActivityService {
    private static final int MAX_FEED_CANDIDATES = 100;
    private final CommunityEventRepository events;
    private final ActivityContext context;

    /** 必须加入来源事务；不能先提交动态再让业务操作失败。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            String key,
            long actor,
            CommunityEventKind kind,
            CommunitySubjectType subjectType,
            String subjectId) {
        events.append(
                new CommunityEvent(
                        null,
                        key,
                        actor,
                        kind,
                        subjectType,
                        subjectId,
                        OffsetDateTime.now(ZoneOffset.UTC)));
    }

    /** 游标分页读取可展示动态；筛选先于分页，目标权限仍由原业务模块决定。 */
    @Transactional(readOnly = true)
    public CommunityActivityPage recent(int size, CommunityActivityFilter filter, Long before) {
        context.requireMember();
        // 每次扫描有上限；隐藏目标也推进游标，避免空页反复请求同一批历史记录。
        int scanLimit = Math.min(size * 10, MAX_FEED_CANDIDATES);
        List<CommunityEvent> rows = events.recent(scanLimit, before, filter.kinds());
        Map<Long, Target> targets = context.targets(rows);
        Set<Long> actors =
                rows.stream()
                        .filter(row -> targets.containsKey(row.id()))
                        .map(CommunityEvent::actorId)
                        .collect(Collectors.toSet());
        Map<Long, String> names = context.names(actors);
        Map<Long, String> avatars = context.avatars(actors);
        List<CommunityActivityView> items = new ArrayList<>();
        int consumed = 0;
        Long lastScannedId = null;
        for (CommunityEvent row : rows) {
            consumed++;
            lastScannedId = row.id();
            Target target = targets.get(row.id());
            if (target == null || !names.containsKey(row.actorId())) continue;
            items.add(
                    new CommunityActivityView(
                            row.id().toString(),
                            Long.toString(row.actorId()),
                            names.get(row.actorId()),
                            avatars.get(row.actorId()),
                            row.kind(),
                            target.title(),
                            target.content(),
                            target.href(),
                            row.createdAt()));
            if (items.size() == size) break;
        }
        // 只把已消费的最后 ID 作为游标，剩余候选留给下一页；不可见记录不会堵住滚动。
        boolean hasMore = consumed < rows.size() || rows.size() == scanLimit;
        return new CommunityActivityPage(
                List.copyOf(items),
                hasMore && lastScannedId != null ? lastScannedId.toString() : null);
    }
}
