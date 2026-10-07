package com.aries.backend.catalog.application.port;

import java.util.Map;
import java.util.Set;

/** 阅读互动身份端口；由 composition 接入账号模块，不让 catalog 依赖身份实现。 */
public interface PublicationReaderIdentity {
    /** 批量获取头像签名，未设置头像的账号不出现在映射中。 */
    Map<Long, String> avatarUrls(Set<Long> ids);

    Map<Long, String> displayNames(Set<Long> ids);

    long requireUserId();

    Long optionalUserId();
}
