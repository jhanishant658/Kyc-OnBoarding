package com.kyc.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a KYC submission by a merchant.
 * Contains personal info, business info, and file paths for uploaded documents.
 */
@Entity
@Table(name = "kyc_submissions")
@Data
@NoArgsConstructor
public class KycSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The merchant who created this submission
    @ManyToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant;

    // Personal info
    private String name;
    private String email;
    private String phone;

    // Business info
    private String businessName;
    private String businessType;
    private String expectedMonthlyVolume;

    // File paths (stored as relative paths on disk)
    private String panFilePath;
    private String aadhaarFilePath;
    private String bankStatementFilePath;

    // Current status in the state machine
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status = KycStatus.DRAFT;

    // Reviewer notes (used for rejection reason or more-info requests)
    private String reviewerNotes;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // When submission entered the queue (moved to SUBMITTED)
    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum KycStatus {
        DRAFT,
        SUBMITTED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        MORE_INFO_REQUESTED
    }
}
