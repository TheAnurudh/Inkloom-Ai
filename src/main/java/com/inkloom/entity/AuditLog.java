package com.inkloom.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Append-only history of important actions ("who did what, when"). */
@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_org", columnList = "org_id"))
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;
    @Column(length = 150) private String actorEmail;
    private Long orgId;
    @Column(nullable = false, length = 60) private String action;
    @Column(length = 60) private String entityType;
    private Long entityId;
    @Column(length = 500) private String details;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected AuditLog() { }
    public AuditLog(Long actorId, String actorEmail, Long orgId, String action, String entityType, Long entityId, String details) {
        this.actorId = actorId; this.actorEmail = actorEmail; this.orgId = orgId; this.action = action;
        this.entityType = entityType; this.entityId = entityId; this.details = details;
    }

    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public Long getOrgId() { return orgId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
