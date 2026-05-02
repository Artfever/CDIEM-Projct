package com.project.service;

import com.project.model.Evidence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates and compares SHA-256 hashes for evidence integrity checks.
 */
public class HashService {
    private static final int BUFFER_SIZE = 8192;

    public String generateSHA256(Path filePath) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            // Stream the file in chunks so large evidence files do not have to be loaded fully into memory.
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return toHex(digest.digest());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the evidence file for hashing.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 hashing is not available in this runtime.", e);
        }
    }

    public String retrieveStoredHash(Evidence evidence) {
        return evidence.getOriginalSha256();
    }

    public String recalculateHash(Evidence evidence) {
        return generateSHA256(Path.of(evidence.getStoredFilePath()));
    }

    public boolean compareHashes(String storedHash, String recalculatedHash) {
        // Hash comparison is case-insensitive because hex strings may be stored with different casing.
        return storedHash != null
                && recalculatedHash != null
                && storedHash.equalsIgnoreCase(recalculatedHash);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
