package com.scms.scms.repository;

import com.scms.scms.model.PortalNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PortalNotificationRepository extends JpaRepository<PortalNotification, Long> {
    List<PortalNotification> findByRecipientRoleAndRecipientEmailAndScheduledForLessThanEqualOrderByCreatedAtDesc(
            String recipientRole, String recipientEmail, LocalDateTime now);

    long countByRecipientRoleAndRecipientEmailAndScheduledForLessThanEqualAndReadAtIsNull(
            String recipientRole, String recipientEmail, LocalDateTime now);

    List<PortalNotification> findByRecipientRoleAndRecipientEmailOrderByCreatedAtDesc(
            String recipientRole, String recipientEmail);

    List<PortalNotification> findByRecipientRoleAndCategoryOrderByCreatedAtDesc(
            String recipientRole, String category);
}
