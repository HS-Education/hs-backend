package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkWithMetadata;
import com.hs.hstesis.shared.infrastructure.ai.DocumentReferenceFormatter;
import java.util.LinkedHashMap;
import java.util.List;

/** Adapts authorized retrieved documents to the shared reference formatter. */
final class ChatReferenceFormatter {
    private final DocumentReferenceFormatter delegate;

    ChatReferenceFormatter(List<DocumentChunkWithMetadata> chunks) {
        var titles = new LinkedHashMap<String, String>();
        for (var chunk : chunks) {
            titles.putIfAbsent("/courses/" + chunk.getCourseId() + "/documents/"
                    + chunk.getDocumentId() + "/download", chunk.getTitle());
        }
        delegate = new DocumentReferenceFormatter(titles);
    }

    String accept(String token) { return delegate.accept(token); }
    String finish() { return delegate.finish(); }

    static String format(String answer, List<DocumentChunkWithMetadata> chunks) {
        var formatter = new ChatReferenceFormatter(chunks);
        return formatter.accept(answer) + formatter.finish();
    }
}
