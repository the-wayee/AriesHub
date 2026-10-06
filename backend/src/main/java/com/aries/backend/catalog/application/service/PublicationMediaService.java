package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.*;

import com.aries.backend.catalog.application.command.SavePublicationCommand;
import com.aries.backend.catalog.application.port.*;
import com.aries.backend.catalog.domain.repository.PublicationRepository;
import com.aries.backend.shared.application.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/** 内容素材用例：负责类型、尺寸、绑定与公开/付费授权，通用存储不参与这些业务规则。 */
@Service
@RequiredArgsConstructor
public class PublicationMediaService {
    private final PublicationMediaPort storage;
    private final PublicationAssetRepository assets;
    private final PublicationRepository publications;
    private static final Pattern REFERENCE = Pattern.compile("media:([0-9a-fA-F-]{36})");

    public record Uploaded(
            String id,
            String kind,
            String filename,
            String contentType,
            long size,
            String url,
            java.time.Instant expiresAt) {}

    public Uploaded upload(
            String kind, String filename, String type, long size, InputStream source) {
        boolean image = Set.of("COVER", "IMAGE").contains(kind),
                video = "VIDEO".equals(kind),
                attachment = "ATTACHMENT".equals(kind);
        long limit = (video ? 100 : image ? 10 : 20) * 1024L * 1024;
        if ((!image && !video && !attachment)
                || size <= 0
                || size > limit
                || type == null
                || (image && !Set.of("image/png", "image/jpeg", "image/webp").contains(type))
                || (video && !Set.of("video/mp4", "video/webm").contains(type))
                || (attachment
                        && (filename == null
                                || !filename.toLowerCase(Locale.ROOT)
                                        .matches(
                                                ".*\\.(pdf|zip|txt|csv|json|md|pptx|docx|xlsx)$"))))
            throw new BusinessException(INVALID_MEDIA);
        long userId = storage.currentUserId();
        storage.checkRate(userId);
        try {
            var content = new PushbackInputStream(source, 12);
            byte[] h = content.readNBytes(12);
            boolean valid =
                    attachment
                            || (type.equals("image/png")
                                    && h.length >= 8
                                    && Arrays.equals(
                                            Arrays.copyOf(h, 8),
                                            new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
                            || (type.equals("image/jpeg")
                                    && h.length >= 3
                                    && h[0] == (byte) 255
                                    && h[1] == (byte) 216
                                    && h[2] == (byte) 255)
                            || (type.equals("image/webp")
                                    && h.length >= 12
                                    && new String(
                                                    h,
                                                    0,
                                                    4,
                                                    java.nio.charset.StandardCharsets.US_ASCII)
                                            .equals("RIFF")
                                    && new String(
                                                    h,
                                                    8,
                                                    4,
                                                    java.nio.charset.StandardCharsets.US_ASCII)
                                            .equals("WEBP"))
                            || (type.equals("video/mp4")
                                    && h.length >= 8
                                    && new String(
                                                    h,
                                                    4,
                                                    4,
                                                    java.nio.charset.StandardCharsets.US_ASCII)
                                            .equals("ftyp"))
                            || (type.equals("video/webm")
                                    && h.length >= 4
                                    && h[0] == 0x1a
                                    && h[1] == 0x45
                                    && h[2] == (byte) 0xdf
                                    && h[3] == (byte) 0xa3);
            if (!valid) throw new BusinessException(INVALID_MEDIA);
            content.unread(h);
            var asset = storage.upload(userId, kind, filename, type, size, content);
            assets.save(asset);
            var url = storage.url(asset);
            return new Uploaded(
                    asset.id(),
                    kind,
                    asset.filename(),
                    asset.contentType(),
                    size,
                    url.url(),
                    url.expiresAt());
        } catch (IOException e) {
            throw new BusinessException(INVALID_MEDIA);
        }
    }

    public PublicationMediaPort.SignedUrl adminUrl(String id) {
        return storage.url(
                assets.find(id).orElseThrow(() -> new BusinessException(MEDIA_NOT_FOUND)));
    }

    public void validateReferences(long publicationId, SavePublicationCommand command) {
        var publicIds = references(command.previewMarkdown());
        if (command.coverFileId() != null) publicIds.add(command.coverFileId());
        var all = references(command.fullMarkdown());
        all.addAll(publicIds);
        long userId = storage.currentUserId();
        for (String id : all) {
            var asset = assets.find(id).orElseThrow(() -> new BusinessException(MEDIA_NOT_FOUND));
            if (asset.ownerId() != userId && !assets.bound(publicationId, id))
                throw new BusinessException(MEDIA_NOT_FOUND);
            if (id.equals(command.coverFileId())
                    && !Set.of("COVER", "IMAGE").contains(asset.kind()))
                throw new BusinessException(INVALID_MEDIA);
        }
    }

    public void bind(long publicationId, SavePublicationCommand command) {
        validateReferences(publicationId, command);
        var publicIds = references(command.previewMarkdown());
        if (command.coverFileId() != null) publicIds.add(command.coverFileId());
        var all = references(command.fullMarkdown());
        all.addAll(publicIds);
        assets.replaceBindings(publicationId, new ArrayList<>(all), new ArrayList<>(publicIds));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PublicationMediaPort.SignedUrl publicUrl(long publicationId, String id) {
        var p =
                publications
                        .findById(publicationId)
                        .orElseThrow(() -> new BusinessException(PUBLICATION_NOT_FOUND));
        if (!p.isPubliclyVisible()) throw new BusinessException(PUBLICATION_NOT_FOUND);
        if (!assets.bound(publicationId, id)) throw new BusinessException(MEDIA_NOT_FOUND);
        if (!assets.publiclyVisible(publicationId, id) && !p.allowsPublicReading())
            throw new BusinessException(CONTENT_LOCKED);
        return adminUrl(id);
    }

    private Set<String> references(String markdown) {
        Set<String> result = new LinkedHashSet<>();
        if (markdown != null) {
            var m = REFERENCE.matcher(markdown);
            while (m.find()) result.add(m.group(1));
        }
        return result;
    }
}
