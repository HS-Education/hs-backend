package com.hs.hstesis.repo.interfaces.rest.advice;

import com.hs.hstesis.repo.domain.exceptions.InvalidPdfUploadException;
import com.hs.hstesis.repo.domain.exceptions.MalwareScannerUnavailableException;
import com.hs.hstesis.repo.domain.exceptions.PdfUploadTooLargeException;
import com.hs.hstesis.shared.interfaces.rest.resources.ApiErrorResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Order(-10)
@RestControllerAdvice
public class PdfUploadExceptionHandler {
    @ExceptionHandler(InvalidPdfUploadException.class)
    public ResponseEntity<ApiErrorResponse> invalid(InvalidPdfUploadException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(400, "Invalid PDF", ex.getMessage()));
    }

    @ExceptionHandler({PdfUploadTooLargeException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiErrorResponse> tooLarge(Exception ignored) {
        return ResponseEntity.status(413).body(new ApiErrorResponse(413, "Upload too large",
                "The upload exceeds the 50 MiB limit."));
    }

    @ExceptionHandler(MalwareScannerUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> scannerUnavailable(MalwareScannerUnavailableException ignored) {
        return ResponseEntity.status(503).body(new ApiErrorResponse(503, "Scanner unavailable",
                "Document scanning is temporarily unavailable."));
    }
}
