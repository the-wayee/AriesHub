package com.aries.backend.catalog.application.service;

import static com.aries.backend.catalog.application.exception.CatalogErrorCode.PUBLICATION_NOT_FOUND;

import com.aries.backend.catalog.application.event.PublicationActivityOccurred;
import com.aries.backend.catalog.application.port.PublicationCoverPort;
import com.aries.backend.catalog.application.port.PublicationMediaPort;
import com.aries.backend.catalog.application.port.PublicationReaderIdentity;
import com.aries.backend.catalog.application.port.PublicationReaderRepository;
import com.aries.backend.catalog.application.port.PublicationShareRepository;
import com.aries.backend.catalog.application.port.PublicationShareRepository.Share;
import com.aries.backend.catalog.application.view.CatalogViews.Content;
import com.aries.backend.catalog.application.view.CatalogViews.PublicationDetail;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;
import com.aries.backend.catalog.application.view.PublicationReaderViews.ShareLink;
import com.aries.backend.catalog.application.view.PublicationReaderViews.SharedPublication;
import com.aries.backend.catalog.domain.model.Publication.AccessType;
import com.aries.backend.catalog.domain.model.PublicationEventKind;
import com.aries.backend.shared.application.exception.BusinessException;
import com.aries.backend.shared.application.port.SiteAddress;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** 分享令牌只允许读取绑定文章；不能扩展为社区浏览权限，也不能替代付费解锁。 */
@Service
@RequiredArgsConstructor
public class PublicationShareService {
    private final PublicationShareRepository shares;
    private final PublicationReaderRepository readers;
    private final PublicationReaderIdentity identity;
    private final CatalogQueryService catalog;
    private final PublicationCoverPort covers;
    private final PublicationMediaService media;
    private final SiteAddress site;
    private final ApplicationEventPublisher events;

    /** 生成当前用户对该文章的专属分享令牌；重复获取复用授权，不在此处增加分享计数。 */
    @Transactional
    public ShareLink link(long id) {
        long user = identity.requireUserId();
        if (!readers.lockVisible(id)) throw new BusinessException(PUBLICATION_NOT_FOUND);
        PublicationDetail detail = catalog.detail(id);
        Share share = shares.getOrCreate(user, id);
        return new ShareLink(
                Long.toString(id),
                site.absolutePath("/s/" + share.token()),
                share.token(),
                detail.publication().coverFileId() == null
                        ? null
                        : covers.sign(List.of(detail.publication().coverFileId()))
                                .get(detail.publication().coverFileId()),
                detail.publication().summary());
    }

    /** 确认当前用户拥有此令牌后按唯一授权计数，不能替别人确认或重复刷次数。 */
    @Transactional
    public Interaction confirm(long id, String token) {
        long user = identity.requireUserId();
        if (!readers.lockVisible(id)) throw new BusinessException(PUBLICATION_NOT_FOUND);
        Share share = requireShare(token);
        if (share.publicationId() != id || share.userId() != user)
            throw new BusinessException(PUBLICATION_NOT_FOUND);
        // 点击分享按授权唯一计数：反复点击、复制、刷新或并发确认不会累加。
        if (readers.event(user, id, PublicationEventKind.SHARE, "share:" + share.id()))
            events.publishEvent(
                    new PublicationActivityOccurred(
                            "share:" + share.id(),
                            user,
                            id,
                            PublicationActivityOccurred.Kind.SHARE));
        return readers.interactions(List.of(id), user).get(Long.toString(id));
    }

    /** 分享页公开展示文章信息与分享者；令牌不替代付费正文的账号权益。 */
    @Transactional(readOnly = true)
    public SharedPublication preview(String token) {
        Share share = requireShare(token);
        PublicationDetail detail = catalog.detail(share.publicationId());
        Long viewer = identity.optionalUserId();
        boolean canRead =
                AccessType.FREE.name().equals(detail.publication().accessType())
                        || (viewer != null && readers.unlocked(viewer, share.publicationId()));
        String coverId = detail.publication().coverFileId();
        return new SharedPublication(
                detail,
                coverId == null ? null : covers.sign(List.of(coverId)).get(coverId),
                identity.displayNames(Set.of(share.userId())).getOrDefault(share.userId(), "社区成员"),
                canRead,
                identity.avatarUrls(Set.of(share.userId())).get(share.userId()));
    }

    @Transactional(readOnly = true)
    public Content content(String token) {
        Share share = requireShare(token);
        PublicationDetail detail = catalog.detail(share.publicationId());
        // 令牌只对免费内容授予匿名读取。付费内容始终走账号身份与已有解锁权益校验。
        return AccessType.FREE.name().equals(detail.publication().accessType())
                ? catalog.content(share.publicationId())
                : catalog.memberContent(share.publicationId());
    }

    @Transactional(readOnly = true)
    public PublicationMediaPort.SignedUrl media(String token, String fileId) {
        Share share = requireShare(token);
        boolean authorized = preview(token).canRead();
        return media.readerUrl(share.publicationId(), fileId, authorized);
    }

    @Transactional(readOnly = true)
    public List<PublicationMediaService.Attachment> attachments(String token) {
        Share share = requireShare(token);
        content(token);
        return media.attachments(share.publicationId(), false).stream()
                .map(
                        item ->
                                new PublicationMediaService.Attachment(
                                        item.id(),
                                        item.filename(),
                                        item.contentType(),
                                        item.size(),
                                        false))
                .toList();
    }

    private Share requireShare(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
            throw new BusinessException(PUBLICATION_NOT_FOUND);
        Share share =
                shares.find(token).orElseThrow(() -> new BusinessException(PUBLICATION_NOT_FOUND));
        catalog.detail(share.publicationId());
        return share;
    }
}
