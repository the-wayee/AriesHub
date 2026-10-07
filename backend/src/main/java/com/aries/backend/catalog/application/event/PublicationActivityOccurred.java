package com.aries.backend.catalog.application.event;

/** 已成功写入的内容行为；去重键由用例产生，不接受客户端伪造动态。 */
public record PublicationActivityOccurred(
        String eventKey, long actorId, long publicationId, Kind kind) {
    public enum Kind {
        LIKE,
        BOOKMARK,
        SHARE,
        PUBLISHED
    }
}
