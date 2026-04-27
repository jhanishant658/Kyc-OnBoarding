package com.kyc.controller;

import com.kyc.dto.KycSubmissionDto;
import com.kyc.dto.ReviewActionDto;
import com.kyc.entity.KycSubmission.KycStatus;
import com.kyc.repository.KycSubmissionRepository;
import com.kyc.service.KycSubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Reviewer-facing API endpoints.
 * Only accessible by users with ROLE_REVIEWER (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/reviewer")
public class ReviewerController {

    private final KycSubmissionService submissionService;
    private final KycSubmissionRepository submissionRepository;

    public ReviewerController(KycSubmissionService submissionService,
                               KycSubmissionRepository submissionRepository) {
        this.submissionService = submissionService;
        this.submissionRepository = submissionRepository;
    }

    // Get the review queue (SUBMITTED and UNDER_REVIEW), oldest first
    @GetMapping("/queue")
    public ResponseEntity<?> getQueue() {
        List<KycSubmissionDto> queue = submissionService.getReviewQueue();
        return ResponseEntity.ok(queue);
    }

    // Get a single submission detail
    @GetMapping("/submissions/{id}")
    public ResponseEntity<?> getSubmission(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(submissionService.getSubmissionForReviewer(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Get all submissions (all merchants)
    @GetMapping("/submissions")
    public ResponseEntity<?> getAllSubmissions() {
        List<KycSubmissionDto> all = submissionRepository.findAll().stream()
                .map(submissionService::toDto)
                .toList();
        return ResponseEntity.ok(all);
    }

    /**
     * Reviewer takes action on a submission.
     * Valid newStatus values: UNDER_REVIEW, APPROVED, REJECTED, MORE_INFO_REQUESTED
     */
    @PatchMapping("/submissions/{id}/action")
    public ResponseEntity<?> takeAction(@PathVariable Long id,
                                         @RequestBody ReviewActionDto actionDto) {
        try {
            KycStatus newStatus = KycStatus.valueOf(actionDto.getNewStatus());
            KycSubmissionDto updated = submissionService.reviewerTransition(id, newStatus, actionDto.getNotes());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + actionDto.getNewStatus()));
        } catch (IllegalStateException e) {
            // Invalid state machine transition
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Dashboard stats for the reviewer.
     * Returns: queue size, average queue time, approval rate for last 7 days.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        List<KycSubmissionDto> queue = submissionService.getReviewQueue();

        // Calculate average time submissions have been in the queue
        double avgQueueHours = queue.stream()
                .filter(s -> s.getSubmittedAt() != null)
                .mapToLong(s -> Duration.between(s.getSubmittedAt(), LocalDateTime.now()).toHours())
                .average()
                .orElse(0);

        // Approval rate over last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long approvedLast7Days = submissionRepository.countApprovedSince(sevenDaysAgo);
        long submittedLast7Days = submissionRepository.countSubmittedSince(sevenDaysAgo);
        double approvalRate = submittedLast7Days > 0
                ? (double) approvedLast7Days / submittedLast7Days * 100
                : 0;

        return ResponseEntity.ok(Map.of(
                "queueSize", queue.size(),
                "avgQueueTimeHours", Math.round(avgQueueHours * 10.0) / 10.0,
                "approvalRateLast7Days", Math.round(approvalRate * 10.0) / 10.0,
                "approvedLast7Days", approvedLast7Days,
                "submittedLast7Days", submittedLast7Days
        ));
    }
}
