package com.inkloom.service;

import com.inkloom.entity.AuditLog;
import com.inkloom.entity.User;
import com.inkloom.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes the audit trail. Runs inside the caller's transaction (if the action fails, no audit row either). */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    @Transactional
    public void write(User actor, Long orgId, String action, String entityType, Long entityId, String details) {
        String safe = details == null ? null : (details.length() > 480 ? details.substring(0, 480) : details);
        repo.save(new AuditLog(actor == null ? null : actor.getId(), actor == null ? "system" : actor.getEmail(),
                orgId, action, entityType, entityId, safe));
        log.debug("[AUDIT] {} by={} org={} {}#{} {}", action, actor == null ? "system" : actor.getEmail(), orgId, entityType, entityId, safe);
    }
}
