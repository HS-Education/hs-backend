package com.hs.hstesis.shared.domain.model.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class TextUtils {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String normalize(String input) {
        if (input == null) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        return DIACRITICS_PATTERN.matcher(normalized)
                .replaceAll("")
                .toUpperCase()
                .trim();
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isBlank()) return "";

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder titleCase = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                titleCase.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return titleCase.toString().trim();
    }
}
