package com.kyc.service;

import com.kyc.dto.KycSubmissionDto;
import com.kyc.entity.KycSubmission;
import com.kyc.entity.KycSubmission.KycStatus;
import com.kyc.entity.User;
import com.kyc.repository.KycSubmissionRepository;
import com.kyc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main service for KYC submission operations.
 * Handles creating, updating, and fetching submissions.
 */
@Service
public class KycSubmissionService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final KycSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final KycStateMachineService stateMachineService;
    private final FileValidationService fileValidationService;

    public KycSubmissionService(KycSubmissionRepository submissionRepository,
                                 UserRepository userRepository,
                                 KycStateMachineService stateMachineService,
                                 FileValidationService fileValidationService) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.stateMachineService = stateMachineService;
        this.fileValidationService = fileValidationService;
    }

    // Get all submissions for a merchant
    public List<KycSubmissionDto> getSubmissionsForMerchant(String username) {
        User merchant = findUser(username);
        return submissionRepository.findByMerchant(merchant).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Get a single submission - only if it belongs to the merchant
    public KycSubmissionDto getSubmissionForMerchant(Long id, String username) {
        KycSubmission sub = findSubmission(id);
        if (!sub.getMerchant().getUsername().equals(username)) {
            throw new SecurityException("Access denied: this submission does not belong to you.");
        }
        return toDto(sub);
    }

    // Get a submission for reviewer (no ownership check)
    public KycSubmissionDto getSubmissionForReviewer(Long id) {
        return toDto(findSubmission(id));
    }

    // Create a new draft submission
    public KycSubmissionDto createDraft(String username, KycSubmissionDto dto) {
        User merchant = findUser(username);
        KycSubmission submission = new KycSubmission();
        submission.setMerchant(merchant);
        updateFields(submission, dto);
        submission.setStatus(KycStatus.DRAFT);
        return toDto(submissionRepository.save(submission));
    }

    // Update an existing draft (merchant only, only if status is DRAFT or MORE_INFO_REQUESTED)
    public KycSubmissionDto updateDraft(Long id, String username, KycSubmissionDto dto) {
        KycSubmission submission = findSubmission(id);
        if (!submission.getMerchant().getUsername().equals(username)) {
            throw new SecurityException("Access denied.");
        }
        if (submission.getStatus() != KycStatus.DRAFT && submission.getStatus() != KycStatus.MORE_INFO_REQUESTED) {
            throw new IllegalStateException("Cannot edit a submission that is not in DRAFT or MORE_INFO_REQUESTED status.");
        }
        updateFields(submission, dto);
        return toDto(submissionRepository.save(submission));
    }

    // Submit a draft for review
    public KycSubmissionDto submitForReview(Long id, String username) {
        KycSubmission submission = findSubmission(id);
        if (!submission.getMerchant().getUsername().equals(username)) {
            throw new SecurityException("Access denied.");
        }
        return toDto(stateMachineService.transition(submission, KycStatus.SUBMITTED, null));
    }

    // Upload a file and return its saved path
    public String uploadFile(MultipartFile file, String fieldName) throws IOException {
        fileValidationService.validate(file, fieldName);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // Generate a unique filename to avoid collisions
        String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueName);
        file.transferTo(filePath.toFile());

        return uniqueName;
    }

    // Get the review queue (all reviewers can access)
    public List<KycSubmissionDto> getReviewQueue() {
        return submissionRepository.findReviewQueue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Reviewer transitions a submission
    public KycSubmissionDto reviewerTransition(Long id, KycStatus newStatus, String notes) {
        KycSubmission submission = findSubmission(id);
        return toDto(stateMachineService.transition(submission, newStatus, notes));
    }

    // --- Private helpers ---

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private KycSubmission findSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + id));
    }

    private void updateFields(KycSubmission submission, KycSubmissionDto dto) {
        if (dto.getName() != null) submission.setName(dto.getName());
        if (dto.getEmail() != null) submission.setEmail(dto.getEmail());
        if (dto.getPhone() != null) submission.setPhone(dto.getPhone());
        if (dto.getBusinessName() != null) submission.setBusinessName(dto.getBusinessName());
        if (dto.getBusinessType() != null) submission.setBusinessType(dto.getBusinessType());
        if (dto.getExpectedMonthlyVolume() != null) submission.setExpectedMonthlyVolume(dto.getExpectedMonthlyVolume());
        if (dto.getPanFilePath() != null) submission.setPanFilePath(dto.getPanFilePath());
        if (dto.getAadhaarFilePath() != null) submission.setAadhaarFilePath(dto.getAadhaarFilePath());
        if (dto.getBankStatementFilePath() != null) submission.setBankStatementFilePath(dto.getBankStatementFilePath());
    }

    /**
     * Convert entity to DTO.
     * Also calculates the SLA atRisk flag dynamically (not stored in DB).
     */
    public KycSubmissionDto toDto(KycSubmission sub) {
        KycSubmissionDto dto = new KycSubmissionDto();
        dto.setId(sub.getId());
        dto.setName(sub.getName());
        dto.setEmail(sub.getEmail());
        dto.setPhone(sub.getPhone());
        dto.setBusinessName(sub.getBusinessName());
        dto.setBusinessType(sub.getBusinessType());
        dto.setExpectedMonthlyVolume(sub.getExpectedMonthlyVolume());
        dto.setPanFilePath(sub.getPanFilePath());
        dto.setAadhaarFilePath(sub.getAadhaarFilePath());
        dto.setBankStatementFilePath(sub.getBankStatementFilePath());
        dto.setStatus(sub.getStatus().name());
        dto.setReviewerNotes(sub.getReviewerNotes());
        dto.setCreatedAt(sub.getCreatedAt());
        dto.setUpdatedAt(sub.getUpdatedAt());
        dto.setSubmittedAt(sub.getSubmittedAt());
        dto.setMerchantUsername(sub.getMerchant().getUsername());

        // SLA: if submitted more than 24 hours ago and still in queue, mark at risk
        // This is calculated dynamically - we do NOT store this in the DB
        if (sub.getSubmittedAt() != null &&
            (sub.getStatus() == KycStatus.SUBMITTED || sub.getStatus() == KycStatus.UNDER_REVIEW)) {
            long hoursInQueue = Duration.between(sub.getSubmittedAt(), LocalDateTime.now()).toHours();
            dto.setAtRisk(hoursInQueue > 24);
        } else {
            dto.setAtRisk(false);
        }

        return dto;
    }
}
