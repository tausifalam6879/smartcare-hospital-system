package com.raahmediq.medicalrecord.storage;

import java.util.UUID;

public interface PrivateDocumentStorage {
    StoredDocument store(UUID patientId, String originalFilename, byte[] content);
    byte[] load(String storageKey);
    void delete(String storageKey);

    record StoredDocument(String storageKey, String detectedContentType, long sizeBytes, String sha256) {
    }
}
