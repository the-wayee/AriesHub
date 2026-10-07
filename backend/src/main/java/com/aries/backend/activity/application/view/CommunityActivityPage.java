package com.aries.backend.activity.application.view;

import java.util.List;

/** 向下加载动态；nextCursor 是最后扫描的事件 ID，null 表示已到末尾。 */
public record CommunityActivityPage(List<CommunityActivityView> items, String nextCursor) {}
