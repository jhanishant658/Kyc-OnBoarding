package com.kyc.controller;

import com.kyc.dto.KycSubmissionDto;
import com.kyc.service.KycSubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Merchant-facing API endpoints.
 * All endpoints here are only accessible to authenticated merchants.
 * Each merchant can only see their own submissions (enforced in service layer).
 */
@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantController {

    private final KycSubmissionService submissionService;

    public MerchantController(KycSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    // Get all submissions for the logged-in merchant
    @GetMapping("/submissions")
    public ResponseEntity<?> getMySubmissions(Authentication auth) {
        return ResponseEntity.ok(submissionService.getSubmissionsForMerchant(auth.getName()));
    }

    // Get one submission (ownership verified in service)
    @GetMapping("/submissions/{id}")
    public ResponseEntity<?> getSubmission(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(submissionService.getSubmissionForMerchant(id, auth.getName()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(error(e.getMessage()));
        }
    }

    // Create a new draft submission
    @PostMapping("/submissions")
    public ResponseEntity<?> createDraft(@RequestBody KycSubmissionDto dto, Authentication auth) {
        return ResponseEntity.ok(submissionService.createDraft(auth.getName(), dto));
    }

    // Update an existing draft
    @PutMapping("/submissions/{id}")
    public ResponseEntity<?> updateDraft(@PathVariable Long id,
                                          @RequestBody KycSubmissionDto dto,
                                          Authentication auth) {
        try {
            return ResponseEntity.ok(submissionService.updateDraft(id, auth.getName(), dto));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    // Submit a draft for review
    @PostMapping("/submissions/{id}/submit")
    public ResponseEntity<?> submitForReview(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(submissionService.submitForReview(id, auth.getName()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            // Return 400 for invalid state transitions
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    /**
     * Upload a file for a submission field.
     * Returns the saved file path which should then be set on the submission DTO.
     *
     * Usage: POST /api/v1/merchant/upload?field=pan
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "field", defaultValue = "document") String field) {
        try {
            String filePath = submissionService.uploadFile(file, field);
            return ResponseEntity.ok(Map.of("filePath", filePath, "field", field));
        } catch (IllegalArgumentException e) {
            // File validation failed
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("File upload failed: " + e.getMessage()));
        }
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
