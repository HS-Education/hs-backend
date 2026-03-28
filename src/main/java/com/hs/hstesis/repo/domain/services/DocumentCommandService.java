package com.hs.hstesis.repo.domain.services;

import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.domain.model.commands.DeleteDocumentCommand;
import com.hs.hstesis.repo.domain.model.commands.SaveDocumentEmbeddingsCommand;
import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;

public interface DocumentCommandService {
    Long handle(UploadDocumentCommand command, UploadFile file);
    void handle(DeleteDocumentCommand command);
    void handle(SaveDocumentEmbeddingsCommand command);
}
