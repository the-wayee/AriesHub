package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;

import java.util.List;
import java.util.Map;

/** 经调用方完成公开范围筛选或管理员鉴权后的封面签名批量获取；外部存储失败不掩盖文章列表。 */
public interface PublicationCoverPort {
    Map<String, SignedUrl> sign(List<String> fileIds);
}
