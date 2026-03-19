package com.hs.hstesis.repo.domain.model.valueobjects;

public enum DocumentFormat {
    PDF;

    public static DocumentFormat fromFileName(String fileName) {

        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1)
                .toLowerCase();

        return switch (extension) {
            case "pdf" -> PDF;
            default -> throw new IllegalArgumentException("Unsupported file format: " + extension);
        };
    }
}
