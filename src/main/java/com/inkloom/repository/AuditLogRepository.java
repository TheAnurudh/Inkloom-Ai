package com.inkloom.repository;

import com.inkloom.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrgIdOrderByCreatedAtDesc(Long orgId, Pageable pageable);
}
