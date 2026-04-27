package com.kyc.repository;

import com.kyc.entity.KycSubmission;
import com.kyc.entity.KycSubmission.KycStatus;
import com.kyc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for KYC submissions.
 * Contains custom queries for the reviewer dashboard.
 */
public interface KycSubmissionRepository extends JpaRepository<KycSubmission, Long> {

    // Get all submissions by a specific merchant
    List<KycSubmission> findByMerchant(User merchant);

    // Get all submissions with a given status, oldest first (for the review queue)
    List<KycSubmission> findByStatusOrderBySubmittedAtAsc(KycStatus status);

    // Count submissions approved in the last 7 days (for approval rate metric)
    @Query("SELECT COUNT(s) FROM KycSubmission s WHERE s.status = 'APPROVED' AND s.updatedAt >= :since")
    long countApprovedSince(@Param("since") LocalDateTime since);

    // Count submissions submitted in the last 7 days (for approval rate metric)
    @Query("SELECT COUNT(s) FROM KycSubmission s WHERE s.submittedAt >= :since")
    long countSubmittedSince(@Param("since") LocalDateTime since);

    // Find all submissions in SUBMITTED or UNDER_REVIEW (the review queue)
    @Query("SELECT s FROM KycSubmission s WHERE s.status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY s.submittedAt ASC")
    List<KycSubmission> findReviewQueue();
}
