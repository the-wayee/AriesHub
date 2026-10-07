package com.aries.backend.shared.application.port;

/** 网站公开地址的基础能力，不依赖请求 Host，也不包含具体业务链接规则。 */
public interface SiteAddress {
    String absolutePath(String path);
}
