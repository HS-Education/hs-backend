package com.hs.hstesis.shared.infrastructure.ai;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Streams complete lines, keeping model-written source labels out of the response. */
public final class DocumentReferenceFormatter {
    private static final Pattern SOURCE = Pattern.compile("(?i)(?:\\*\\*)?(?:Fuente|Source)(?:\\*\\*)?:(?:\\*\\*)?");
    private static final Pattern LINK = Pattern.compile("/courses/(\\d+)/documents/(\\d+)/download");
    private static final Pattern MARKDOWN_LINK = Pattern.compile(
            "\\[[^\\]\\n]*\\]\\([^\\n)]*(/courses/\\d+/documents/\\d+/download)\\)");
    private static final Pattern DOWNLOAD_URL = Pattern.compile(
            "(?:https?://[^\\s/]+)?(?:/api/v1)?(/courses/\\d+/documents/\\d+/download)");
    private static final Pattern INLINE_CITATION = Pattern.compile("【[^】]*(?:】|$)");
    private final Map<String, String> references = new LinkedHashMap<>();
    private final Set<String> cited = new LinkedHashSet<>();
    private final StringBuilder pending = new StringBuilder();

    public DocumentReferenceFormatter(Map<String, String> titlesByPath) {
        titlesByPath.forEach((path, title) -> references.put(path,
                "Fuente: " + title + " Enlace de descarga: /api/v1" + path));
    }

    public String accept(String token) {
        pending.append(token);
        var result = new StringBuilder();
        int newline;
        while ((newline = pending.indexOf("\n")) >= 0) {
            result.append(clean(pending.substring(0, newline))).append("\n");
            pending.delete(0, newline + 1);
        }
        return result.toString();
    }

    public String finish() {
        String tail = clean(pending.toString());
        pending.setLength(0);
        if (cited.isEmpty()) return tail;
        return tail + "\n\n" + cited.stream().map(references::get)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String clean(String line) {
        var inline = INLINE_CITATION.matcher(line);
        var withoutCitations = new StringBuffer();
        while (inline.find()) {
            var links = LINK.matcher(inline.group());
            while (links.find()) {
                if (references.containsKey(links.group())) cited.add(links.group());
            }
            inline.appendReplacement(withoutCitations, "");
        }
        inline.appendTail(withoutCitations);
        line = withoutCitations.toString().replace("】", "");
        if (line.matches("(?i)^[ \t#*]*(?:fuentes consultadas|referencias?|sources|references)[: \t*]*$")) return "";
        var source = SOURCE.matcher(line);
        boolean sourceLabel = source.find();
        boolean downloadLabel = line.matches("(?i)^[ \t]*(?:[-*•][ \t]*)?(?:\\*\\*)?(?:Enlace de descarga|Download link)(?:\\*\\*)?:.*");
        if (!sourceLabel && !downloadLabel) {
            var markdown = MARKDOWN_LINK.matcher(line);
            var cleaned = new StringBuffer();
            while (markdown.find()) {
                if (references.containsKey(markdown.group(1))) cited.add(markdown.group(1));
                markdown.appendReplacement(cleaned, "");
            }
            markdown.appendTail(cleaned);
            var urls = DOWNLOAD_URL.matcher(cleaned.toString());
            var withoutUrls = new StringBuffer();
            while (urls.find()) {
                if (references.containsKey(urls.group(1))) cited.add(urls.group(1));
                urls.appendReplacement(withoutUrls, "");
            }
            urls.appendTail(withoutUrls);
            return withoutUrls.toString();
        }
        int start = sourceLabel ? source.start() : 0;
        var links = LINK.matcher(line.substring(start));
        while (links.find()) {
            if (references.containsKey(links.group())) cited.add(links.group());
        }
        // Drop the source label and quoted extract, preserving any preceding answer text.
        return line.substring(0, start)
                .replaceFirst("[ \t]*(?:[-*•]|\\[\\d+\\])?[ \t]*$", "");
    }

    public static String format(String answer, Map<String, String> titlesByPath) {
        var formatter = new DocumentReferenceFormatter(titlesByPath);
        return formatter.accept(answer) + formatter.finish();
    }
}
