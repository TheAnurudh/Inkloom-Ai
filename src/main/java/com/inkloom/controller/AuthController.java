package com.inkloom.controller;

import com.inkloom.dto.Dtos.*;
import com.inkloom.security.CustomUserDetails;
import com.inkloom.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    /** 202: always the same generic answer. The verification link arrives by email. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest req) { return auth.register(req); }

    /** Frontend page /verify-email?token=... calls this with the token from the URL. */
    @PostMapping("/verify-email")
    public MessageResponse verify(@Valid @RequestBody VerifyEmailRequest req) { return auth.verifyEmail(req); }

    @PostMapping("/resend-verification")
    public MessageResponse resend(@Valid @RequestBody ResendVerificationRequest req) { return auth.resendVerification(req); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) { return auth.login(req); }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CustomUserDetails me) { return auth.me(me.getId()); }
}
