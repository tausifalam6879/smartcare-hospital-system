package com.raahmediq.medicalrecord.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class LocalPrivateDocumentStorage implements PrivateDocumentStorage {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private final Path root;

    public LocalPrivateDocumentStorage(@Value("${raahmediq.documents.storage-path:./var/private-documents}") String path) {
        this.root = Path.of(path).toAbsolutePath().normalize();
    }

    @Override
    public StoredDocument store(UUID patientId, String originalFilename, byte[] content) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("Document file is empty.");
        if (content.length > MAX_BYTES) throw new IllegalArgumentException("Document must be 10 MB or smaller.");
        DetectedType detected = detect(content);
        String key = patientId + "/" + UUID.randomUUID() + detected.extension();
        Path target = safePath(key);
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try {
            Files.createDirectories(target.getParent());
            Files.write(temporary, content);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredDocument(key, detected.contentType(), content.length, sha256(content));
        } catch (IOException exception) {
            throw new IllegalStateException("Private document storage is unavailable.", exception);
        }
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(safePath(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("Stored document content is unavailable.", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(safePath(storageKey));
        } catch (IOException ignored) {
            // An orphan cleanup job can retry; never mask the original database failure.
        }
    }

    private Path safePath(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid private storage key.");
        return resolved;
    }

    private static DetectedType detect(byte[] bytes) {
        if (startsWith(bytes, new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D})) return new DetectedType("application/pdf", ".pdf");
        if (startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) return new DetectedType("image/jpeg", ".jpg");
        if (startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return new DetectedType("image/png", ".png");
        }
        throw new IllegalArgumentException("Only genuine PDF, JPG, and PNG medical documents are accepted.");
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) if (value[index] != prefix[index]) return false;
        return true;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }

    private record DetectedType(String contentType, String extension) {
    }
}
