package com.kyc.dto;

import lombok.Data;

/**
 * DTO for reviewer actions (approve, reject, request more info)
 */
@Data
public class ReviewActionDto {
    private String newStatus; // "APPROVED", "REJECTED", "UNDER_REVIEW", "MORE_INFO_REQUESTED"
    private String notes;     // Optional reviewer notes/reason
}
