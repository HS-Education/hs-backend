package com.hs.hstesis.repo.domain.services;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.queries.GetAccessibleDocumentsQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentDownloadQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentsByCoordinatorQuery;

import java.util.List;

public interface DocumentQueryService {
    List<Document> handle(GetAccessibleDocumentsQuery query);
    String handle(GetDocumentDownloadQuery query);
}
