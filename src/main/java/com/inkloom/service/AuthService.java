package com.inkloom.service;

import com.inkloom.dto.Dtos.*;
import com.inkloom.entity.User;
import com.inkloom.exception.ApiException;
import com.inkloom.repository.UserRepository;
import com.inkloom.security.JwtService;
import com.inkloom.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Registration, email verification and login. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Same answer for new / existing emails -> attackers cannot find out who is registered. */
    private static final String REGISTER_MESSAGE =
            "If this email can be registered, a verification link has been sent. Please check your inbox.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final MapperService mapper;
    private final EmailVerificationService verification;
    private final int maxFailedAttempts;
    private final long lockMinutes;
    private final String dummyHash;                    // makes "unknown email" take as long as "wrong password"

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit,
                       MapperService mapper, EmailVerificationService verification,
                       @Value("${inkloom.security.max-failed-attempts}") int maxFailedAttempts,
                       @Value("${inkloom.security.lock-minutes}") long lockMinutes) {
        this.users = users; this.encoder = encoder; this.jwt = jwt; this.audit = audit; this.mapper = mapper;
        this.verification = verification;
        this.maxFailedAttempts = maxFailedAttempts; this.lockMinutes = lockMinutes;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public MessageResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String name = InputSanitizer.plain(req.fullName());
        if (name == null || name.length() < 2) throw ApiException.badRequest("Please enter a valid name");
        if (req.password().equalsIgnoreCase(email)) throw ApiException.badRequest("Password must not be your email");

        User existing = users.findByEmail(email).orElse(null);
        if (existing == null) {
            User u = users.save(new User(name, email, encoder.encode(req.password()), req.accountType()));
            audit.write(u, null, "USER_REGISTERED", "User", u.getId(), "accountType=" + u.getAccountType());
            log.info("[AUTH] registered userId={} email={} (waiting for email verification)", u.getId(), email);
            verification.issueAndSend(u);
        } else if (!existing.isEmailVerified()) {
            // Latest submission wins while unverified, then a fresh link goes to the mailbox owner.
            existing.overwriteUnverified(name, encoder.encode(req.password()), req.accountType());
            users.save(existing);
            log.info("[AUTH] re-registration of unverified email {} -> new verification mail", email);
            verification.issueAndSend(existing);
        } else {
            encoder.matches(req.password(), dummyHash);                    // equalise timing
            log.warn("[AUTH] register ignored: {} is already registered and verified", email);
        }
        return new MessageResponse(REGISTER_MESSAGE);
    }

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest req) {
        verification.verify(req.token());
        return new MessageResponse("Email verified. You can log in now.");
    }

    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        User u = users.findByEmail(email).orElse(null);
        if (u != null && !u.isEmailVerified()) verification.issueAndSend(u);
        else encoder.matches("x", dummyHash);                              // same timing, same answer
        return new MessageResponse("If this email is waiting for verification, a new link has been sent.");
    }

    /**
     * NOT @Transactional on purpose: when login fails we throw, which would roll back the
     * "failed attempt" counter. Each repository.save() below commits by itself.
     */
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email).orElse(null);

        if (user == null) {
            encoder.matches(req.password(), dummyHash);
            log.warn("[AUTH] login failed: unknown email {}", email);
            throw new BadCredentialsException("Invalid email or password");
        }
        if (user.isLocked()) {
            log.warn("[AUTH] login blocked: account locked until {} ({})", user.getLockUntil(), email);
            throw new LockedException("Account temporarily locked after too many failed attempts. Try again later.");
        }
        if (!user.isEnabled()) {
            log.warn("[AUTH] login blocked: disabled account {}", email);
            throw new DisabledException("Account disabled");
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            user.registerFailedAttempt(maxFailedAttempts, lockMinutes);
            users.save(user);
            log.warn("[AUTH] login failed: wrong password for {} (failedAttempts={}, lockUntil={})",
                    email, user.getFailedAttempts(), user.getLockUntil());
            throw new BadCredentialsException("Invalid email or password");
        }
        // Password is correct. Only now we reveal that the email still needs verification.
        if (!user.isEmailVerified()) {
            log.warn("[AUTH] login blocked: email not verified ({})", email);
            throw ApiException.forbidden("Please verify your email first. Check your inbox or request a new link.");
        }

        if (user.getFailedAttempts() > 0 || user.getLockUntil() != null) {
            user.resetFailedAttempts();
            users.save(user);
        }
        String token = jwt.generateToken(user);
        log.info("[AUTH] login OK userId={} email={}", user.getId(), email);
        return new AuthResponse("Bearer", token, jwt.getExpirationSeconds(), mapper.toUser(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return mapper.toUser(users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")));
    }
}
