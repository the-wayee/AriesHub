package com.aries.backend.discussion.application.view;

import java.time.OffsetDateTime;
import java.util.List;

/** 接口直接返回树节点；parentId 仍保留，便于客户端定位回复对象。 */
public record CommentNode(String id, String parentId, int depth, String authorId,
                          String authorName, String body, OffsetDateTime createdAt,
                          List<CommentNode> replies) {}
