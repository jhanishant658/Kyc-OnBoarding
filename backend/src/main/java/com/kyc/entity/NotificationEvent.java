package com.kyc.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores notification events whenever a KYC status changes.
 * No real email sending — just DB storage for audit purposes.
 */
@Entity
@Table(name = "notification_events")
@Data
@NoArgsConstructor
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long merchantId;

    private String eventType; // e.g., "STATUS_CHANGED_TO_APPROVED"

    private LocalDateTime timestamp;

    @Column(length = 1000)
    private String payload; // JSON-like string with extra details

    public NotificationEvent(Long merchantId, String eventType, String payload) {
        this.merchantId = merchantId;
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }
}
