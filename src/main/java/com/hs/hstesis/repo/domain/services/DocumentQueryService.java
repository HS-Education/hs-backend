package com.hs.hstesis.repo.domain.services;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.queries.GetAccessibleDocumentsQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentByIdQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentDownloadQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentsByCoordinatorQuery;

import java.util.List;
import java.util.Optional;

public interface DocumentQueryService {
    Optional<Document> handle(GetDocumentByIdQuery query);
    List<Document> handle(GetAccessibleDocumentsQuery query);
    String handle(GetDocumentDownloadQuery query);
}
