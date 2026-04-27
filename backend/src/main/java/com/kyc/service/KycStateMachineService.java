package com.kyc.service;

import com.kyc.entity.KycSubmission;
import com.kyc.entity.KycSubmission.KycStatus;
import com.kyc.entity.NotificationEvent;
import com.kyc.repository.KycSubmissionRepository;
import com.kyc.repository.NotificationEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * STATE MACHINE SERVICE
 *
 * This is the ONLY place where KYC status transitions happen.
 * All allowed transitions are defined here. No transition logic exists in controllers.
 *
 * Allowed transitions:
 *   DRAFT             -> SUBMITTED
 *   SUBMITTED         -> UNDER_REVIEW
 *   UNDER_REVIEW      -> APPROVED
 *   UNDER_REVIEW      -> REJECTED
 *   UNDER_REVIEW      -> MORE_INFO_REQUESTED
 *   MORE_INFO_REQUESTED -> SUBMITTED
 */
@Service
public class KycStateMachineService {

    private final KycSubmissionRepository submissionRepository;
    private final NotificationEventRepository notificationRepository;

    // Define which transitions are allowed: current status -> set of allowed next statuses
    private static final Map<KycStatus, Set<KycStatus>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        ALLOWED_TRANSITIONS.put(KycStatus.DRAFT, Set.of(KycStatus.SUBMITTED));
        ALLOWED_TRANSITIONS.put(KycStatus.SUBMITTED, Set.of(KycStatus.UNDER_REVIEW));
        ALLOWED_TRANSITIONS.put(KycStatus.UNDER_REVIEW, Set.of(
                KycStatus.APPROVED,
                KycStatus.REJECTED,
                KycStatus.MORE_INFO_REQUESTED
        ));
        ALLOWED_TRANSITIONS.put(KycStatus.MORE_INFO_REQUESTED, Set.of(KycStatus.SUBMITTED));
        // Terminal states - no transitions out
        ALLOWED_TRANSITIONS.put(KycStatus.APPROVED, Set.of());
        ALLOWED_TRANSITIONS.put(KycStatus.REJECTED, Set.of());
    }

    public KycStateMachineService(KycSubmissionRepository submissionRepository,
                                   NotificationEventRepository notificationRepository) {
        this.submissionRepository = submissionRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Attempt to transition a submission to a new status.
     * Throws IllegalStateException if the transition is not allowed.
     */
    public KycSubmission transition(KycSubmission submission, KycStatus newStatus, String notes) {
        KycStatus currentStatus = submission.getStatus();

        // Check if this transition is allowed
        Set<KycStatus> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedNext.contains(newStatus)) {
            throw new IllegalStateException(
                    "Invalid transition from " + currentStatus + " to " + newStatus +
                    ". Allowed transitions: " + allowedNext
            );
        }

        // Apply the transition
        submission.setStatus(newStatus);
        submission.setReviewerNotes(notes);

        // Track when the submission entered the queue
        if (newStatus == KycStatus.SUBMITTED) {
            submission.setSubmittedAt(LocalDateTime.now());
        }

        // Save updated submission
        KycSubmission saved = submissionRepository.save(submission);

        // Store a notification event (no actual email sent)
        saveNotificationEvent(submission, newStatus, notes);

        return saved;
    }

    // Helper: save a notification event to the DB
    private void saveNotificationEvent(KycSubmission submission, KycStatus newStatus, String notes) {
        String payload = String.format(
                "{\"submissionId\":%d, \"newStatus\":\"%s\", \"notes\":\"%s\"}",
                submission.getId(),
                newStatus,
                notes != null ? notes : ""
        );
        NotificationEvent event = new NotificationEvent(
                submission.getMerchant().getId(),
                "STATUS_CHANGED_TO_" + newStatus.name(),
                payload
        );
        notificationRepository.save(event);
    }
}
