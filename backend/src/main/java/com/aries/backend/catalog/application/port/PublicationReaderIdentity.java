package com.aries.backend.catalog.application.port;

/** 阅读互动身份端口；由 composition 接入账号模块，不让 catalog 依赖身份实现。 */
public interface PublicationReaderIdentity {
    long requireUserId();

    Long optionalUserId();
}
