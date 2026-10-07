package com.aries.backend.shared.infrastructure.config;

import com.aries.backend.shared.application.port.SiteAddress;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

/** 公开域名由项目环境配置；拒绝用户信息、查询参数或子路径，避免错误分享链接。 */
@Component
public class ConfiguredSiteAddress implements SiteAddress {
    private final URI origin;

    public ConfiguredSiteAddress(@Value("${app.site-url}") String siteUrl) {
        URI parsed = URI.create(siteUrl.trim());
        if ((!"http".equalsIgnoreCase(parsed.getScheme())
                        && !"https".equalsIgnoreCase(parsed.getScheme()))
                || parsed.getHost() == null
                || parsed.getUserInfo() != null
                || parsed.getQuery() != null
                || parsed.getFragment() != null
                || (!parsed.getPath().isEmpty() && !"/".equals(parsed.getPath()))) {
            throw new IllegalArgumentException("SITE_URL 必须是完整的 http/https 网站域名，不含路径或参数");
        }
        this.origin = parsed;
    }

    public String absolutePath(String path) {
        return origin.resolve(path).toString();
    }
}
