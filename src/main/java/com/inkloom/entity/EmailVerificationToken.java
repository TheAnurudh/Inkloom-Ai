package com.inkloom.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One-time email verification token. Only the SHA-256 HASH is stored (a DB leak cannot be used to verify accounts). */
@Entity
@Table(name = "email_verification_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_evt_hash", columnNames = "token_hash"))
public class EmailVerificationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected EmailVerificationToken() { }
    public EmailVerificationToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }

    public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
}
