package com.kyc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for KYC submissions.
 * Used for both API requests (input) and responses (output).
 */
@Data
public class KycSubmissionDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String businessName;
    private String businessType;
    private String expectedMonthlyVolume;
    private String panFilePath;
    private String aadhaarFilePath;
    private String bankStatementFilePath;
    private String status;
    private String reviewerNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private String merchantUsername;

    // SLA field - calculated dynamically, never stored in DB
    private boolean atRisk;
}
