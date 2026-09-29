package com.hs.hstesis.repo.domain.services;

import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.domain.model.commands.DeleteDocumentCommand;
import com.hs.hstesis.repo.domain.model.commands.SaveDocumentEmbeddingsCommand;
import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;
import com.hs.hstesis.repo.domain.model.commands.UploadBulkDocumentsCommand;
import java.util.List;

public interface DocumentCommandService {
    Long handle(UploadDocumentCommand command, UploadFile file);
    void handle(DeleteDocumentCommand command);
    void retryProcessing(com.hs.hstesis.repo.domain.model.commands.RetryDocumentProcessingCommand command);
    void handle(SaveDocumentEmbeddingsCommand command);
    List<Long> handle(UploadBulkDocumentsCommand command, List<UploadFile> uploadFiles);
}
