package com.aries.backend.composition;

import com.aries.backend.catalog.application.port.PublicationCoverPort;
import com.aries.backend.catalog.application.port.PublicationMediaPort.SignedUrl;
import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.storage.application.service.FileStorageService;
import com.aries.backend.storage.domain.model.StoredFile;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 仅签名查询用例已筛选公开的封面；一次获取元数据，签名不需要向 OSS 发起上传/下载。 */
@Component
@RequiredArgsConstructor
public class PublicationCoverAdapter implements PublicationCoverPort {
    private final FileStorageService files;

    public Map<String, SignedUrl> sign(List<String> ids) {
        Map<String, SignedUrl> result = new HashMap<>();
        for (StoredFile file : files.metadata(ids.stream().map(UUID::fromString).toList())) {
            try {
                FileStorageService.Download url = files.inlineUrl(file);
                result.put(file.id().toString(), new SignedUrl(url.url(), url.expiresAt()));
            } catch (BusinessException unavailable) {
                /* 列表仍可显示文字，单素材入口支持用户重新加载。 */
            }
        }
        return result;
    }
}
