package com.hs.hstesis.repo.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class DocumentNotFoundException extends ResourceNotFoundException {
    public DocumentNotFoundException(Long documentId) {
        super("Document", documentId);
    }
}
