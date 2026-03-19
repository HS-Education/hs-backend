package com.hs.hstesis.repo.domain.services;

import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;

public interface DocumentCommandService {
    Long handle(UploadDocumentCommand command);
}
