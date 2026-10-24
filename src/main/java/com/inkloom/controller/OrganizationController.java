package com.inkloom.controller;

import com.inkloom.dto.Dtos.*;
import com.inkloom.security.CustomUserDetails;
import com.inkloom.service.OrganizationService;
import com.inkloom.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Company workspace: teams, employees, approval rules, products, review panel, audit. */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService orgs;
    private final ProductService products;

    public OrganizationController(OrganizationService orgs, ProductService products) { this.orgs = orgs; this.products = products; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody CreateOrganizationRequest req) {
        return orgs.createOrganization(me.getId(), req);
    }

    @GetMapping("/mine")
    public List<OrganizationResponse> mine(@AuthenticationPrincipal CustomUserDetails me) { return orgs.myOrganizations(me.getId()); }

    @PostMapping("/{orgId}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse createTeam(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId, @Valid @RequestBody CreateTeamRequest req) {
        return orgs.createTeam(me.getId(), orgId, req);
    }

    @PostMapping("/{orgId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId, @Valid @RequestBody AddMemberRequest req) {
        return orgs.addMember(me.getId(), orgId, req);
    }

    @GetMapping("/{orgId}/members")
    public List<MemberResponse> members(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId) {
        return orgs.listMembers(me.getId(), orgId);
    }

    @PutMapping("/{orgId}/rules")
    public ApprovalRuleResponse setRule(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId, @Valid @RequestBody ApprovalRuleRequest req) {
        return orgs.upsertRule(me.getId(), orgId, req);
    }

    @GetMapping("/{orgId}/rules")
    public List<ApprovalRuleResponse> rules(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId) {
        return orgs.listRules(me.getId(), orgId);
    }

    @PostMapping("/{orgId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId, @Valid @RequestBody CreateProductRequest req) {
        return products.create(me.getId(), orgId, req);
    }

    @GetMapping("/{orgId}/content")
    public List<ContentResponse> content(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId) {
        return orgs.orgContent(me.getId(), orgId);
    }

    @GetMapping("/{orgId}/audit")
    public List<AuditLogResponse> audit(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long orgId,
                                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return orgs.auditLogs(me.getId(), orgId, page, size);
    }
}
