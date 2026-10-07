package com.aries.backend.activity.application.query;

import com.aries.backend.activity.domain.model.CommunityEventKind;

import java.util.List;

/** 动态 Tab 的业务筛选；空类型集合代表全部，其他分组在数据库分页前筛选。 */
public enum CommunityActivityFilter {
    ALL,
    COMMENTS,
    PUBLICATIONS,
    INTERACTIONS,
    MEMBERS;

    public List<CommunityEventKind> kinds() {
        return switch (this) {
            case ALL -> List.of();
            case COMMENTS ->
                    List.of(
                            CommunityEventKind.DISCUSSION_COMMENTED,
                            CommunityEventKind.DISCUSSION_REPLIED);
            case PUBLICATIONS -> List.of(CommunityEventKind.PUBLICATION_PUBLISHED);
            case INTERACTIONS ->
                    List.of(
                            CommunityEventKind.PUBLICATION_LIKE,
                            CommunityEventKind.PUBLICATION_BOOKMARK,
                            CommunityEventKind.PUBLICATION_SHARE,
                            CommunityEventKind.DISCUSSION_LIKED);
            case MEMBERS -> List.of(CommunityEventKind.MEMBER_JOINED);
        };
    }
}
