package com.inkloom.security;

import com.inkloom.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/** Creates and verifies JWT tokens (HS256). Tokens are short lived; signature + issuer + expiry are always checked. */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String DEV_SECRET_PREFIX = "dev-only-secret";

    private final SecretKey key;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(@Value("${inkloom.jwt.secret}") String secret,
                      @Value("${inkloom.jwt.expiration-minutes}") long expirationMinutes,
                      @Value("${inkloom.jwt.issuer}") String issuer) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("inkloom.jwt.secret must be at least 32 characters. Set env var JWT_SECRET.");
        }
        if (secret.startsWith(DEV_SECRET_PREFIX)) {
            log.warn("[JWT] Using the built-in DEV secret. Set the JWT_SECRET environment variable before deploying!");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        log.debug("[JWT] issued token for userId={} valid {} min", user.getId(), expirationMinutes);
        return token;
    }

    /** Throws JwtException if the token is forged, expired, from another issuer or malformed. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                .parseSignedClaims(token).getPayload();
    }

    public long getExpirationSeconds() { return expirationMinutes * 60; }
}
