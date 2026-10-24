package com.inkloom.entity;

import com.inkloom.enums.AccountType;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** A registered person. Table name is "users" because "user" is reserved in some databases. */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;                       // always stored lowercase

    @Column(nullable = false, length = 100)
    private String passwordHash;                // BCrypt hash, never the real password

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AccountType accountType;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean emailVerified = false;      // login is blocked until the email link is used

    @Column(nullable = false)
    private int failedAttempts = 0;             // brute-force protection counter

    private Instant lockUntil;                  // account locked until this time (null = not locked)

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() { }

    public User(String fullName, String email, String passwordHash, AccountType accountType) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
    }

    public boolean isLocked() { return lockUntil != null && lockUntil.isAfter(Instant.now()); }

    public void registerFailedAttempt(int maxAttempts, long lockMinutes) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            lockUntil = Instant.now().plus(lockMinutes, ChronoUnit.MINUTES);
            failedAttempts = 0;
        }
    }

    public void resetFailedAttempts() { failedAttempts = 0; lockUntil = null; }

    public void markEmailVerified() { this.emailVerified = true; }

    /** Re-registration of an UNVERIFIED email: the latest submission wins (stops account pre-hijacking). */
    public void overwriteUnverified(String fullName, String passwordHash, AccountType type) {
        this.fullName = fullName; this.passwordHash = passwordHash; this.accountType = type;
    }

    public boolean isEmailVerified() { return emailVerified; }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public AccountType getAccountType() { return accountType; }
    public boolean isEnabled() { return enabled; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockUntil() { return lockUntil; }
    public Instant getCreatedAt() { return createdAt; }
}
