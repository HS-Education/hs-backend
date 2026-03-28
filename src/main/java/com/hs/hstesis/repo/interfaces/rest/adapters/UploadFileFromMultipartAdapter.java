package com.hs.hstesis.repo.interfaces.rest.adapters;

import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

public class UploadFileFromMultipartAdapter implements UploadFile {
    private final MultipartFile multipartFile;

    public UploadFileFromMultipartAdapter(MultipartFile multipartFile) {
        this.multipartFile = Objects.requireNonNull(multipartFile, "multipartFile cannot be null");
    }

    @Override
    public String originalFileName() {
        String name = multipartFile.getOriginalFilename();
        return (name == null || name.isBlank()) ? "unknown" : name;
    }

    @Override
    public String contentType() {
        String contentType = multipartFile.getContentType();
        return (contentType == null || contentType.isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType;
    }

    @Override
    public long size() {
        return multipartFile.getSize();
    }

    @Override
    public InputStream openStream() {
        try {
            return multipartFile.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot open input stream from multipart file", e);
        }
    }
}
