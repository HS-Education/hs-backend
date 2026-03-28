package com.hs.hstesis.repo.application.internal.outboundservices.storage;

import java.io.InputStream;

public interface UploadFile {
    String originalFileName();
    String contentType();
    long size();
    InputStream openStream();
}
