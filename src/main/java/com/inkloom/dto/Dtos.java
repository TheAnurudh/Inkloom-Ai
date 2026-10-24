package com.inkloom.dto;

import com.inkloom.enums.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

/**
 * Every request/response object of the API in one file.
 * Requests carry validation annotations; invalid input is rejected with HTTP 400 before any service code runs.
 */
public final class Dtos {

    private Dtos() { }

    // ============================ AUTH ============================
    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[\\x21-\\x7E]{8,64}$",
                    message = "Password must be 8-64 characters with upper, lower, digit and special character, and no spaces")
            String password,
            @NotNull AccountType accountType) { }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 64) String password) { }

    public record UserResponse(Long id, String fullName, String email, AccountType accountType) { }

    public record MessageResponse(String message) { }

    public record VerifyEmailRequest(@NotBlank @Size(max = 100) String token) { }

    public record ResendVerificationRequest(@NotBlank @Email @Size(max = 150) String email) { }

    public record AuthResponse(String tokenType, String accessToken, long expiresInSeconds, UserResponse user) { }

    // ============================ ORGANIZATION ============================
    public record CreateOrganizationRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @Size(max = 500) String description) { }

    public record OrganizationResponse(Long id, String name, String description, List<OrgRole> myRoles) { }

    public record CreateTeamRequest(@NotBlank @Size(min = 2, max = 80) String name) { }

    public record TeamResponse(Long id, String name) { }

    public record AddMemberRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotNull OrgRole role,
            Long teamId) { }

    public record MemberResponse(Long memberId, Long userId, String fullName, String email, OrgRole role, String teamName) { }

    public record ApprovalRuleRequest(
            @NotNull ContentType contentType,
            @NotNull OrgRole firstReviewerRole,
            OrgRole finalApproverRole) { }

    public record ApprovalRuleResponse(Long id, ContentType contentType, OrgRole firstReviewerRole, OrgRole finalApproverRole) { }

    // ============================ PRODUCT ============================
    public record CreateProductRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String brand,
            @Size(max = 2000) String description,
            @Size(max = 30) List<@NotBlank @Size(max = 300) String> approvedClaims,
            @Size(max = 30) List<@NotBlank @Size(max = 300) String> forbiddenClaims) { }

    public record ProductResponse(Long id, Long organizationId, String organizationName, String name, String brand,
                                  String description, List<String> approvedClaims, List<String> forbiddenClaims) { }

    // ============================ CONTENT + AI ============================
    public record AnalyzeRequest(
            @NotNull Long productId,
            @NotNull ContentType contentType,
            @NotBlank @Size(max = 5000) String script) { }

    public record CreateContentRequest(
            @NotBlank @Size(min = 2, max = 150) String title,
            @NotNull Long productId,
            @NotNull ContentType contentType,
            @NotBlank @Size(max = 5000) String script) { }

    public record UpdateContentRequest(
            @Size(max = 150) String title,
            @NotBlank @Size(max = 5000) String script) { }

    public record RedFlagResponse(String issue, String reason, String suggestedAction, RiskLevel severity) { }

    public record AiReportResponse(int riskScore, RiskLevel riskLevel, boolean meetsCriteria, String summary,
                                   List<RedFlagResponse> flags, Instant analyzedAt, String source) { }

    public record ContentResponse(Long id, String title, ContentType contentType, ContentStatus status, RiskLevel riskLevel,
                                  Long productId, String productName, Long organizationId, String organizationName,
                                  String submittedByName, String script, int currentStep, List<OrgRole> approvalChain,
                                  AiReportResponse latestReport, Instant createdAt, Instant updatedAt) { }

    // ============================ REVIEW ============================
    public record ReviewDecisionRequest(
            @NotNull Decision decision,
            @Size(max = 1000) String comment) { }

    public record ReviewTaskResponse(Long taskId, int step, OrgRole requiredRole, TaskStatus status,
                                     ContentResponse content, Instant createdAt) { }

    public record DecisionResult(Long taskId, Decision decision, ContentStatus contentStatus, String message) { }

    // ============================ AUDIT ============================
    public record AuditLogResponse(Long id, Long actorId, String actorEmail, String action, String entityType,
                                   Long entityId, String details, Instant createdAt) { }
}
