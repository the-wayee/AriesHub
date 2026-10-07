package com.aries.backend.catalog.domain.repository;

import com.aries.backend.catalog.domain.model.Publication;

import java.util.Optional;

/** 发布内容聚合的仓储契约；领域层只认识聚合，不认识 Mapper 或持久化对象。 */
public interface PublicationRepository {
    /** 按主键读取不含正文的聚合；缺失或逻辑删除时返回 empty。 */
    Optional<Publication> findById(long id);

    /** 加载完整正文供编辑和目录投影，调用方负责管理员或公开可见性校验。 */
    Optional<Publication> findForEditing(long id);

    /** 保存聚合与正文；新聚合由数据库分配 ID，返回带 ID 的持久化结果。 */
    Publication save(Publication publication);
}
