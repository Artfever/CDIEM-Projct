package com.project.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SecureFileStorage {
    private static final Path STORAGE_ROOT = Path.of("storage", "evidence");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public Path storeSecurely(Path sourceFile, int caseId) {
        if (sourceFile == null) {
            throw new IllegalArgumentException("A source evidence file is required.");
        }

        if (!Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("The selected evidence file does not exist.");
        }

        try {
            Path caseDirectory = STORAGE_ROOT.resolve("case-" + caseId);
            Files.createDirectories(caseDirectory);

            String originalName = sourceFile.getFileName().toString();
            String storedFileName = FILE_STAMP.format(LocalDateTime.now())
                    + "-"
                    + UUID.randomUUID().toString().replace("-", "")
                    + "-"
                    + sanitizeFileName(originalName);
            Path targetPath = caseDirectory.resolve(storedFileName).toAbsolutePath().normalize();
            Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath;
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the evidence file in secure storage.", e);
        }
    }

    public void deleteQuietly(Path storedFile) {
        if (storedFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(storedFile);
        } catch (IOException ignored) {
        }
    }

    private String sanitizeFileName(String originalName) {
        String sanitized = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "evidence.bin" : sanitized;
    }
}
