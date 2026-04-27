package com.kyc.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Validates uploaded files.
 * Only allows PDF, JPG, PNG. Max size 5MB.
 */
@Service
public class FileValidationService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    /**
     * Validates the file. Throws IllegalArgumentException if invalid.
     * We throw with a clear message so the controller can return a proper 400 response.
     */
    public void validate(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be empty.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    fieldName + " exceeds maximum allowed size of 5MB. Uploaded: " +
                    (file.getSize() / 1024 / 1024) + "MB"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    fieldName + " has invalid file type: " + contentType +
                    ". Allowed types: PDF, JPG, PNG"
            );
        }
    }
}
