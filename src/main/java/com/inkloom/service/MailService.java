package com.inkloom.service;

import com.inkloom.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the verification email.
 * DEV MODE (inkloom.mail.enabled=false): nothing is sent, the link is printed in the console so you can test.
 * PROD MODE (true): real SMTP. A failed send NEVER crashes registration (the user can request a new link).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${inkloom.mail.enabled:false}") boolean enabled,
                       @Value("${inkloom.mail.from}") String from,
                       @Value("${inkloom.frontend-url}") String frontendUrl,
                       @Value("${spring.mail.host:}") String smtpHost) {
        this.senderProvider = senderProvider;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        if (enabled && (smtpHost == null || smtpHost.isBlank())) {
            log.error("[MAIL] inkloom.mail.enabled=true but MAIL_HOST is empty - emails cannot be sent!");
        }
        log.info("[MAIL] mode = {}", enabled ? "REAL SMTP" : "DEV (links are printed in console)");
    }

    public void sendVerification(User user, String rawToken) {
        // The frontend page /verify-email reads ?token= and calls POST /api/auth/verify-email
        String link = frontendUrl + "/verify-email?token=" + rawToken;

        if (!enabled) {
            log.warn("[MAIL][DEV ONLY] verification link for {} -> {}", user.getEmail(), link);
            return;
        }
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            log.error("[MAIL] no JavaMailSender configured (set MAIL_HOST / MAIL_USERNAME / MAIL_PASSWORD)");
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(user.getEmail());
            m.setSubject("Verify your InkLoom email");
            m.setText("Hi " + user.getFullName() + ",\n\nPlease verify your email by opening this link:\n" + link
                    + "\n\nThe link works once and expires soon. If you did not create an InkLoom account, ignore this email.\n");
            sender.send(m);
            log.info("[MAIL] verification email sent to {}", user.getEmail());
        } catch (MailException e) {
            log.error("[MAIL] could not send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
