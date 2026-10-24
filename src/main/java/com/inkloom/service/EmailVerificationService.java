package com.inkloom.service;

import com.inkloom.entity.EmailVerificationToken;
import com.inkloom.entity.User;
import com.inkloom.exception.ApiException;
import com.inkloom.repository.EmailVerificationTokenRepository;
import com.inkloom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/** Creates, emails and checks verification tokens (random 256-bit, stored hashed, single use, expiring). */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final MailService mail;
    private final AuditService audit;
    private final long validHours;

    public EmailVerificationService(EmailVerificationTokenRepository tokens, UserRepository users, MailService mail,
                                    AuditService audit, @Value("${inkloom.mail.verification-hours:24}") long validHours) {
        this.tokens = tokens; this.users = users; this.mail = mail; this.audit = audit; this.validHours = validHours;
    }

    /** Makes a fresh token (old ones are deleted) and emails it. Skipped if one was sent in the last 60 seconds. */
    @Transactional
    public void issueAndSend(User user) {
        var last = tokens.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
        if (last.isPresent() && last.get().getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(Instant.now())) {
            log.info("[VERIFY] cooldown active for {} - not sending another email yet", user.getEmail());
            return;
        }
        tokens.deleteByUserId(user.getId());
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new EmailVerificationToken(user, sha256(rawToken), Instant.now().plus(validHours, ChronoUnit.HOURS)));
        log.debug("[VERIFY] token created for userId={} valid {}h", user.getId(), validHours);
        mail.sendVerification(user, rawToken);
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken t = tokens.findByTokenHash(sha256(rawToken.trim())).orElse(null);
        if (t == null || t.isExpired()) {
            log.warn("[VERIFY] invalid or expired token used");
            throw ApiException.badRequest("Invalid or expired verification link. Please request a new one.");
        }
        User user = t.getUser();
        user.markEmailVerified();
        users.save(user);
        tokens.deleteByUserId(user.getId());                 // single use
        audit.write(user, null, "EMAIL_VERIFIED", "User", user.getId(), null);
        log.info("[VERIFY] email verified for userId={}", user.getId());
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
