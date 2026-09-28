package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.command.SaveCaseCommand;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CategoryOption;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseDetail;
import com.aries.backend.catalog.application.view.AdminCatalogViews.CaseSummary;

import java.util.List;
import java.util.Optional;

/** 内容后台读写端口，应用层不直接依赖 Mapper。 */
public interface AdminCatalogPort {
    List<CategoryOption> categories();
    List<CaseSummary> cases();
    Optional<CaseDetail> find(long id);
    long create(SaveCaseCommand command);
    boolean update(long id, SaveCaseCommand command);
    boolean publish(long id);
    boolean archive(long id);
}
