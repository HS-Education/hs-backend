package com.hs.hstesis.repo.domain.services;

import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentCommandService {
    Long handle(UploadDocumentCommand command, MultipartFile file);
}
