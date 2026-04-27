package com.kyc.service;

import com.kyc.entity.KycSubmission;
import com.kyc.entity.KycSubmission.KycStatus;
import com.kyc.entity.User;
import com.kyc.repository.KycSubmissionRepository;
import com.kyc.repository.NotificationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for KYC state machine transitions.
 * We test both valid and invalid transitions.
 */
@ExtendWith(MockitoExtension.class)
class KycStateMachineServiceTest {

    @Mock
    private KycSubmissionRepository submissionRepository;

    @Mock
    private NotificationEventRepository notificationRepository;

    @InjectMocks
    private KycStateMachineService stateMachineService;

    private KycSubmission submission;

    @BeforeEach
    void setUp() {
        // Create a sample submission for testing
        User merchant = new User("testmerchant", "password", User.Role.MERCHANT);
        merchant.setId(1L);

        submission = new KycSubmission();
        submission.setId(1L);
        submission.setMerchant(merchant);
        submission.setStatus(KycStatus.DRAFT);

        // Mock the save to return the same submission
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testValidTransition_DraftToSubmitted() {
        // This should succeed
        KycSubmission result = stateMachineService.transition(submission, KycStatus.SUBMITTED, null);
        assertEquals(KycStatus.SUBMITTED, result.getStatus());
    }

    @Test
    void testInvalidTransition_DraftToApproved() {
        // DRAFT -> APPROVED is not allowed; should throw
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> stateMachineService.transition(submission, KycStatus.APPROVED, null)
        );
        assertTrue(ex.getMessage().contains("Invalid transition"));
        assertTrue(ex.getMessage().contains("DRAFT"));
        assertTrue(ex.getMessage().contains("APPROVED"));
    }

    @Test
    void testInvalidTransition_ApprovedToRejected() {
        // Once APPROVED, no further transitions allowed
        submission.setStatus(KycStatus.APPROVED);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> stateMachineService.transition(submission, KycStatus.REJECTED, "changing mind")
        );
        assertTrue(ex.getMessage().contains("Invalid transition"));
    }

    @Test
    void testInvalidTransition_SubmittedToApproved() {
        // Must go through UNDER_REVIEW first
        submission.setStatus(KycStatus.SUBMITTED);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> stateMachineService.transition(submission, KycStatus.APPROVED, null)
        );
        assertTrue(ex.getMessage().contains("Invalid transition"));
    }

    @Test
    void testValidTransition_UnderReviewToApproved() {
        submission.setStatus(KycStatus.UNDER_REVIEW);
        KycSubmission result = stateMachineService.transition(submission, KycStatus.APPROVED, "All docs verified");
        assertEquals(KycStatus.APPROVED, result.getStatus());
        assertEquals("All docs verified", result.getReviewerNotes());
    }

    @Test
    void testValidTransition_MoreInfoRequestedToSubmitted() {
        // Merchant resubmits after providing more info
        submission.setStatus(KycStatus.MORE_INFO_REQUESTED);
        KycSubmission result = stateMachineService.transition(submission, KycStatus.SUBMITTED, null);
        assertEquals(KycStatus.SUBMITTED, result.getStatus());
    }

    @Test
    void testInvalidTransition_RejectedToAnything() {
        // REJECTED is a terminal state
        submission.setStatus(KycStatus.REJECTED);

        assertThrows(
                IllegalStateException.class,
                () -> stateMachineService.transition(submission, KycStatus.SUBMITTED, null)
        );
    }
}
