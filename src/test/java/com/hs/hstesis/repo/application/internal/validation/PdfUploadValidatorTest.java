package com.hs.hstesis.repo.application.internal.validation;

import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.domain.exceptions.InvalidPdfUploadException;
import com.hs.hstesis.repo.domain.exceptions.PdfUploadTooLargeException;
import com.hs.hstesis.repo.interfaces.rest.advice.PdfUploadExceptionHandler;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfUploadValidatorTest {
    private final PdfUploadValidator validator = new PdfUploadValidator(bytes -> {});

    private static byte[] pdf(boolean active, boolean encrypted) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            if (active) document.getDocumentCatalog().getCOSObject().setItem(COSName.OPEN_ACTION, new COSDictionary());
            if (encrypted) document.protect(new StandardProtectionPolicy("owner", "user", new AccessPermission()));
            document.save(output);
            return output.toByteArray();
        }
    }

    private static UploadFile file(byte[] bytes, String name, String type) {
        return new UploadFile() {
            public String originalFileName() { return name; }
            public String contentType() { return type; }
            public long size() { return bytes.length; }
            public InputStream openStream() { return new ByteArrayInputStream(bytes); }
        };
    }

    @Test
    void acceptsValidPdfAndReturnsExactlyInspectedBytes() throws Exception {
        byte[] original = pdf(false, false);
        UploadFile safe = validator.validate(file(original, "course.pdf", "application/pdf"), "course.pdf");
        assertThat(safe.openStream().readAllBytes()).isEqualTo(original);
        assertThat(safe.size()).isEqualTo(original.length);
    }

    @Test
    void rejectsCorruptOrDisguisedContent() {
        assertThatThrownBy(() -> validator.validate(file("%PDF-1.7\ntruncated".getBytes(),
                "course.pdf", "application/pdf"), "course.pdf"))
                .isInstanceOf(InvalidPdfUploadException.class);
        assertThatThrownBy(() -> validator.validate(file("not a pdf".getBytes(),
                "course.pdf", "application/pdf"), "course.pdf"))
                .isInstanceOf(InvalidPdfUploadException.class);
    }

    @Test
    void scannerReceivesExactBytesAndDetectionPreventsStorage() throws Exception {
        byte[] original = pdf(false, false);
        final byte[][] scanned = new byte[1][];
        var inspecting = new PdfUploadValidator(bytes -> scanned[0] = bytes);
        UploadFile safe = inspecting.validate(file(original, "course.pdf", "application/pdf"), "course.pdf");
        assertThat(scanned[0]).isEqualTo(original);
        assertThat(safe.openStream().readAllBytes()).isEqualTo(scanned[0]);

        var rejecting = new PdfUploadValidator(bytes -> { throw new InvalidPdfUploadException(); });
        assertThatThrownBy(() -> rejecting.validate(file(original, "course.pdf", "application/pdf"), "course.pdf"))
                .isInstanceOf(InvalidPdfUploadException.class);
    }

    @Test
    void rejectsUnsafeNameMimeAndMetadataMismatch() throws Exception {
        byte[] bytes = pdf(false, false);
        for (String name : new String[] {"../course.pdf", "evil.exe", "a\\b.pdf"}) {
            assertThatThrownBy(() -> validator.validate(file(bytes, name, "application/pdf"), name))
                    .isInstanceOf(InvalidPdfUploadException.class);
        }
        assertThatThrownBy(() -> validator.validate(file(bytes, "course.pdf", "text/plain"), "course.pdf"))
                .isInstanceOf(InvalidPdfUploadException.class);
        assertThatThrownBy(() -> validator.validate(file(bytes, "course.pdf", "application/pdf"), "other.pdf"))
                .isInstanceOf(InvalidPdfUploadException.class);
    }

    @Test
    void rejectsEncryptedAndActiveContent() throws Exception {
        assertThatThrownBy(() -> validator.validate(file(pdf(false, true), "course.pdf", "application/pdf"), "course.pdf"))
                .isInstanceOf(InvalidPdfUploadException.class);
        assertThatThrownBy(() -> validator.validate(file(pdf(true, false), "course.pdf", "application/pdf"), "course.pdf"))
                .isInstanceOf(InvalidPdfUploadException.class);
    }

    @Test
    void refusesOversizedContentBeforeOpeningStreamAndMapsErrorsToSafeHttpCodes() {
        UploadFile oversized = new UploadFile() {
            public String originalFileName() { return "large.pdf"; }
            public String contentType() { return "application/pdf"; }
            public long size() { return PdfUploadValidator.MAX_BYTES + 1L; }
            public InputStream openStream() { throw new AssertionError("Must not read oversized upload"); }
        };
        assertThatThrownBy(() -> validator.validate(oversized, "large.pdf"))
                .isInstanceOf(PdfUploadTooLargeException.class);
        PdfUploadExceptionHandler errors = new PdfUploadExceptionHandler();
        assertThat(errors.invalid(new InvalidPdfUploadException()).getStatusCode().value()).isEqualTo(400);
        assertThat(errors.tooLarge(new PdfUploadTooLargeException()).getStatusCode().value()).isEqualTo(413);
    }
}
