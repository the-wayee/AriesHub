package com.aries.backend.catalog.infrastructure.persistence.mapper;

import com.aries.backend.catalog.application.view.CatalogViews.PublicationSummary;
import com.aries.backend.catalog.application.view.PublicationReaderViews.Interaction;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 跨内容、个人关系和阅读记录的复杂只读查询集中在 XML。 */
@Mapper
public interface PublicationReaderReadMapper {
    List<Interaction> interactions(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    List<PublicationSummary> library(
            @Param("userId") long userId,
            @Param("kind") String kind,
            @Param("offset") int offset,
            @Param("size") int size);

    long libraryCount(@Param("userId") long userId, @Param("kind") String kind);
}
