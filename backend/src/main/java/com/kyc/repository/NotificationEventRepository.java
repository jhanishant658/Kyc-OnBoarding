package com.kyc.repository;

import com.kyc.entity.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
    List<NotificationEvent> findByMerchantIdOrderByTimestampDesc(Long merchantId);
}
