package com.hs.hstesis.repo.interfaces.rest.transform;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.interfaces.rest.resources.DocumentResource;

public class DocumentResourceFromEntityAssembler {
    public static DocumentResource toResourceFromEntity(Document entity) {
        return new DocumentResource(
                entity.getId(),
                entity.getTitle(),
                entity.getTopicId(),
                entity.getFormat(),
                entity.getFileStorageInfo().getOriginalFileName()
        );
    }
}