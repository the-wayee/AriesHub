package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.CONTENT_LOCKED;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.INVALID_MEDIA;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.MEDIA_NOT_FOUND;
import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_NOT_FOUND;
import static com.aries.backend.shared.application.util.MediaTypes.IMAGE_WEBP_VALUE;
import static com.aries.backend.shared.application.util.MediaTypes.VIDEO_MP4_VALUE;
import static com.aries.backend.shared.application.util.MediaTypes.VIDEO_WEBM_VALUE;

import static org.springframework.util.MimeTypeUtils.IMAGE_JPEG_VALUE;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;

import com.aries.backend.catalog.application.command.SavePublicationCommand;
import com.aries.backend.catalog.application.port.PublicationAssetRepository;
import com.aries.backend.catalog.application.port.PublicationMediaPort;
import com.aries.backend.catalog.application.port.PublicationReaderIdentity;
import com.aries.backend.catalog.application.port.PublicationReaderRepository;
import com.aries.backend.catalog.application.port.PublicationUploadProgress;
import com.aries.backend.catalog.domain.model.Publication;
import com.aries.backend.catalog.domain.model.PublicationMediaKind;
import com.aries.backend.catalog.domain.repository.PublicationRepository;
import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.shared.application.util.FileSignatures;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 内容素材用例：负责上传策略、文章绑定与公开/付费授权，文件工具只负责存取。 */
@Service
@RequiredArgsConstructor
public class PublicationMediaService {
    private static final long MAX_IMAGE_BYTES = DataSize.ofMegabytes(10).toBytes();
    private static final long MAX_VIDEO_BYTES = DataSize.ofMegabytes(100).toBytes();
    private static final long MAX_ATTACHMENT_BYTES = DataSize.ofMegabytes(20).toBytes();
    private static final Set<String> IMAGE_TYPES =
            Set.of(IMAGE_PNG_VALUE, IMAGE_JPEG_VALUE, IMAGE_WEBP_VALUE);
    private static final Set<String> VIDEO_TYPES = Set.of(VIDEO_MP4_VALUE, VIDEO_WEBM_VALUE);
    private static final Pattern ATTACHMENT_FILENAME =
            Pattern.compile(".*\\.(pdf|zip|txt|csv|json|md|pptx|docx|xlsx)$");
    private static final Pattern MEDIA_REFERENCE = Pattern.compile("media:([0-9a-fA-F-]{36})");
    private final PublicationMediaPort storage;
    private final PublicationAssetRepository assets;
    private final PublicationRepository publications;
    private final PublicationUploadProgress progress;
    private final PublicationReaderIdentity identity;
    private final PublicationReaderRepository readers;

    public record Uploaded(
            String id,
            String kind,
            String filename,
            String contentType,
            long size,
            String url,
            Instant expiresAt) {}

    /** 先校验声明和文件头，再上传；回填文件头确保对象存储收到完整原始字节。 */
    public Uploaded upload(
            String kind, String filename, String type, long size, InputStream source) {
        return upload(kind, filename, type, size, source, null);
    }

    public Uploaded upload(
            String kind,
            String filename,
            String type,
            long size,
            InputStream source,
            UUID uploadId) {
        PublicationMediaKind mediaKind = parseKind(kind);
        validateUpload(mediaKind, filename, type, size);
        long userId = storage.currentUserId();
        storage.checkRate(userId);
        if (uploadId != null) progress.begin(userId, uploadId, size);
        try {
            PushbackInputStream content =
                    new PushbackInputStream(source, FileSignatures.HEADER_BYTES);
            byte[] header = content.readNBytes(FileSignatures.HEADER_BYTES);
            // 附件通过扩展名白名单和 attachment 下载处置控制；不会作为可执行内容内联。
            boolean valid =
                    mediaKind == PublicationMediaKind.ATTACHMENT
                            || (mediaKind.isImage() && FileSignatures.matchesImage(type, header))
                            || (mediaKind == PublicationMediaKind.VIDEO
                                    && FileSignatures.matchesVideo(type, header));
            if (!valid) throw new BusinessException(INVALID_MEDIA);
            content.unread(header);
            PublicationMediaPort.Asset asset =
                    uploadId == null
                            ? storage.upload(
                                    userId, mediaKind.name(), filename, type, size, content)
                            : storage.upload(
                                    userId,
                                    mediaKind.name(),
                                    filename,
                                    type,
                                    size,
                                    content,
                                    bytes -> progress.confirmed(userId, uploadId, bytes, size));
            assets.save(asset);
            PublicationMediaPort.SignedUrl url = storage.url(asset);
            if (uploadId != null) progress.completed(userId, uploadId, size);
            return new Uploaded(
                    asset.id(),
                    asset.kind(),
                    asset.filename(),
                    asset.contentType(),
                    size,
                    url.url(),
                    url.expiresAt());
        } catch (IOException error) {
            if (uploadId != null) progress.failed(userId, uploadId);
            throw new BusinessException(INVALID_MEDIA);
        } catch (RuntimeException error) {
            if (uploadId != null) progress.failed(userId, uploadId);
            throw error;
        }
    }

    /** 当前管理员只能查询/取消自己的任务；任务编号不能绕过身份授权。 */
    public PublicationUploadProgress.Status uploadStatus(UUID id) {
        return progress.status(storage.currentUserId(), id);
    }

    public void cancelUpload(UUID id) {
        progress.cancel(storage.currentUserId(), id);
    }

    /** 此入口只供已通过管理员守卫的编辑路由使用，不替代公开读取授权。 */
    public PublicationMediaPort.SignedUrl adminUrl(String id) {
        return storage.url(
                assets.find(id).orElseThrow(() -> new BusinessException(MEDIA_NOT_FOUND)));
    }

    /** 保存文章前验证引用：本人素材或文章既有绑定允许使用，其他账号的未绑定素材拒绝。 创建时 publicationId 为零，因此不能借用任何旧文章绑定；封面只允许图片类素材。 */
    public void validateReferences(long publicationId, SavePublicationCommand command) {
        long userId = storage.currentUserId();
        for (String id : referencedIds(publicationId, command)) {
            PublicationMediaPort.Asset asset =
                    assets.find(id).orElseThrow(() -> new BusinessException(MEDIA_NOT_FOUND));
            if (asset.ownerId() != userId && !assets.bound(publicationId, id))
                throw new BusinessException(MEDIA_NOT_FOUND);
            if (attachmentIds(publicationId, command).contains(id)
                    && parseKind(asset.kind()) != PublicationMediaKind.ATTACHMENT
                    && parseKind(asset.kind()) != PublicationMediaKind.IMAGE)
                throw new BusinessException(INVALID_MEDIA);
            if (id.equals(command.coverFileId()) && !parseKind(asset.kind()).isImage())
                throw new BusinessException(INVALID_MEDIA);
        }
    }

    /** 与文章保存共用事务，替换绑定后被移除的素材不能再次通过该文章获取签名。 */
    public void bind(long publicationId, SavePublicationCommand command) {
        validateReferences(publicationId, command);
        assets.replaceBindings(
                publicationId,
                new ArrayList<>(referencedIds(publicationId, command)),
                new ArrayList<>(publicIds(command)),
                attachmentIds(publicationId, command));
    }

    /** 签名签发前检查文章状态和绑定；仅封面与公开预览允许普通匿名读取。 */
    @Transactional(readOnly = true)
    public PublicationMediaPort.SignedUrl publicUrl(long publicationId, String id) {
        return readerUrl(publicationId, id, false);
    }

    /** 共享链接专用用例先校验令牌和全文权限，再传入单次授权；不写入全局登录状态。 */
    PublicationMediaPort.SignedUrl readerUrl(long publicationId, String id, boolean authorized) {
        Publication publication =
                publications
                        .findById(publicationId)
                        .orElseThrow(() -> new BusinessException(PUBLICATION_NOT_FOUND));
        if (!publication.isPubliclyVisible()) throw new BusinessException(PUBLICATION_NOT_FOUND);
        if (!assets.bound(publicationId, id)) throw new BusinessException(MEDIA_NOT_FOUND);
        PublicationMediaPort.Asset asset =
                assets.find(id).orElseThrow(() -> new BusinessException(MEDIA_NOT_FOUND));
        // 附件始终跟随文章阅读权限，即使引用被放入公开试读也不能获得免费签名。
        boolean privateAsset =
                !assets.publiclyVisible(publicationId, id)
                        || parseKind(asset.kind()) == PublicationMediaKind.ATTACHMENT
                        || assets.resourceIds(publicationId).contains(id);
        if (privateAsset && !authorized) {
            long user = identity.requireUserId();
            if (!publication.allowsPublicReading() && !readers.unlocked(user, publicationId))
                throw new BusinessException(CONTENT_LOCKED);
        }
        return adminUrl(id);
    }

    /** 详情只公开附件元数据，绝不在未授权响应中签发下载地址。 */
    public record Attachment(
            String id, String filename, String contentType, long size, boolean locked) {}

    @Transactional(readOnly = true)
    public List<Attachment> attachments(long publicationId, boolean admin) {
        Publication publication =
                publications
                        .findById(publicationId)
                        .orElseThrow(() -> new BusinessException(PUBLICATION_NOT_FOUND));
        if (!admin && !publication.isPubliclyVisible())
            throw new BusinessException(PUBLICATION_NOT_FOUND);
        boolean locked = !admin && !publication.allowsPublicReading();
        List<String> resourceIds = admin ? assets.resourceIds(publicationId) : List.of();
        return assets.attachments(publicationId).stream()
                .filter(asset -> !admin || resourceIds.contains(asset.id()))
                .map(
                        asset ->
                                new Attachment(
                                        asset.id(),
                                        asset.filename(),
                                        asset.contentType(),
                                        asset.size(),
                                        locked))
                .toList();
    }

    private List<String> attachmentIds(long publicationId, SavePublicationCommand command) {
        // 旧客户端不传该字段时保留独立附件；显式空数组表示移除附件区中的引用。
        return command.attachmentIds() == null
                ? assets.resourceIds(publicationId)
                : command.attachmentIds();
    }

    private void validateUpload(
            PublicationMediaKind kind, String filename, String type, long size) {
        long limit =
                switch (kind) {
                    case COVER, IMAGE -> MAX_IMAGE_BYTES;
                    case VIDEO -> MAX_VIDEO_BYTES;
                    case ATTACHMENT -> MAX_ATTACHMENT_BYTES;
                };
        if (type == null
                || size <= 0
                || size > limit
                || (kind.isImage() && !IMAGE_TYPES.contains(type))
                || (kind == PublicationMediaKind.VIDEO && !VIDEO_TYPES.contains(type))
                || (kind == PublicationMediaKind.ATTACHMENT
                        && (filename == null
                                || !ATTACHMENT_FILENAME
                                        .matcher(filename.toLowerCase(Locale.ROOT))
                                        .matches()))) {
            throw new BusinessException(INVALID_MEDIA);
        }
    }

    private PublicationMediaKind parseKind(String kind) {
        try {
            return PublicationMediaKind.valueOf(kind);
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new BusinessException(INVALID_MEDIA);
        }
    }

    private Set<String> referencedIds(long publicationId, SavePublicationCommand command) {
        Set<String> result = references(command.fullMarkdown());
        result.addAll(publicIds(command));
        result.addAll(attachmentIds(publicationId, command));
        return result;
    }

    private Set<String> publicIds(SavePublicationCommand command) {
        Set<String> result = references(command.previewMarkdown());
        if (command.coverFileId() != null) result.add(command.coverFileId());
        return result;
    }

    private Set<String> references(String markdown) {
        Set<String> result = new LinkedHashSet<>();
        if (markdown != null) {
            Matcher matcher = MEDIA_REFERENCE.matcher(markdown);
            while (matcher.find()) result.add(matcher.group(1));
        }
        return result;
    }
}
