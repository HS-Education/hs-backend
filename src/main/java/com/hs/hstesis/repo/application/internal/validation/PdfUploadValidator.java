package com.hs.hstesis.repo.application.internal.validation;

import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.domain.exceptions.InvalidPdfUploadException;
import com.hs.hstesis.repo.domain.exceptions.PdfUploadTooLargeException;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Rejects unsupported PDFs; it is not an antivirus or a PDF sanitizer. */
@Component
public final class PdfUploadValidator {
    public static final int MAX_BYTES = 50 * 1024 * 1024;
    public static final int MAX_PAGES = 300;
    private static final int MAX_OBJECTS = 100_000;
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "JavaScript", "JS", "AA", "OpenAction", "EmbeddedFiles", "EF", "XFA", "RichMediaContent");
    private static final Set<String> FORBIDDEN_ACTIONS = Set.of(
            "JavaScript", "Launch", "SubmitForm", "ImportData", "GoToR", "GoToE", "URI", "Rendition", "Movie", "Sound");

    public UploadFile validate(UploadFile file, String expectedName) {
        if (file == null) throw new InvalidPdfUploadException();
        validateName(expectedName);
        validateName(file.originalFileName());
        if (!expectedName.equals(file.originalFileName()) || !"application/pdf".equalsIgnoreCase(file.contentType()))
            throw new InvalidPdfUploadException();
        if (file.size() > MAX_BYTES) throw new PdfUploadTooLargeException();
        if (file.size() <= 0) throw new InvalidPdfUploadException();
        final byte[] bytes;
        try (InputStream input = file.openStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException | RuntimeException ex) {
            throw new InvalidPdfUploadException();
        }
        if (bytes.length > MAX_BYTES) throw new PdfUploadTooLargeException();
        if (bytes.length != file.size() || bytes.length < 12
                || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
            throw new InvalidPdfUploadException();
        String tail = new String(bytes, Math.max(0, bytes.length - 1024), Math.min(1024, bytes.length), StandardCharsets.ISO_8859_1);
        if (!tail.stripTrailing().endsWith("%%EOF")) throw new InvalidPdfUploadException();
        try (var source = new RandomAccessReadBuffer(bytes)) {
            var parser = new StrictPdfParser(source);
            try (var pdf = parser.parse()) {
                if (pdf.isEncrypted() || pdf.getNumberOfPages() < 1 || pdf.getNumberOfPages() > MAX_PAGES)
                    throw new InvalidPdfUploadException();
                inspect(pdf.getDocument().getTrailer());
                // Resolve page tree and content streams instead of accepting only a readable header/catalog.
                long decodedBytes = 0;
                byte[] buffer = new byte[8192];
                for (var page : pdf.getPages()) {
                    if (page.getMediaBox().getWidth() <= 0 || page.getMediaBox().getHeight() <= 0)
                        throw new InvalidPdfUploadException();
                    try (var content = page.getContents()) {
                        int n;
                        while ((n = content.read(buffer)) != -1) {
                            decodedBytes += n;
                            if (decodedBytes > MAX_BYTES) throw new InvalidPdfUploadException();
                        }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new InvalidPdfUploadException();
        }
        // Storage and checksum must consume the exact bytes inspected, never reopen the original source.
        return new UploadFile() {
            public String originalFileName() { return expectedName; }
            public String contentType() { return "application/pdf"; }
            public long size() { return bytes.length; }
            public InputStream openStream() { return new ByteArrayInputStream(bytes); }
        };
    }

    private void validateName(String name) {
        if (name == null || name.length() > 200 || name.isBlank()
                || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || name.contains("..") || name.matches(".*[\\\\/:*?\"<>|].*")
                || name.codePoints().anyMatch(Character::isISOControl))
            throw new InvalidPdfUploadException();
    }

    private void inspect(COSBase root) throws IOException {
        Set<COSBase> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<COSBase> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            COSBase value = pending.removeFirst();
            if (!seen.add(value)) continue;
            if (seen.size() > MAX_OBJECTS || pending.size() > MAX_OBJECTS) throw new InvalidPdfUploadException();
            if (value instanceof COSObject object) {
                if (object.getObject() != null) pending.add(object.getObject());
            } else if (value instanceof COSDictionary dict) {
                if (FORBIDDEN_ACTIONS.contains(dict.getNameAsString(COSName.S, ""))) throw new InvalidPdfUploadException();
                if ("FileAttachment".equals(dict.getNameAsString(COSName.SUBTYPE))) throw new InvalidPdfUploadException();
                for (var key : dict.keySet()) {
                    if (FORBIDDEN_KEYS.contains(key.getName())) throw new InvalidPdfUploadException();
                    COSBase child = dict.getItem(key);
                    if (child != null) pending.add(child);
                }
            } else if (value instanceof COSArray array) {
                for (COSBase child : array) if (child != null) pending.add(child);
            }
        }
    }

    private static final class StrictPdfParser extends PDFParser {
        private StrictPdfParser(RandomAccessReadBuffer source) throws IOException {
            super(source);
            setLenient(false);
        }
    }
}
