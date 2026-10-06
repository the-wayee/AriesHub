package com.aries.backend.catalog.application.port;

import com.aries.backend.catalog.application.port.PublicationMediaPort.Asset;

import java.util.List;
import java.util.Optional;

public interface PublicationAssetRepository {
    void save(Asset asset);

    Optional<Asset> find(String id);

    boolean bound(long publicationId, String id);

    boolean publiclyVisible(long publicationId, String id);

    void replaceBindings(long publicationId, List<String> all, List<String> publicIds);
}
