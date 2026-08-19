package com.scms.scms.repository;

import com.scms.scms.model.SuperAdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuperAdminAuditLogRepository extends JpaRepository<SuperAdminAuditLog, Long> {
    List<SuperAdminAuditLog> findAllByOrderByCreatedAtDesc();
}
