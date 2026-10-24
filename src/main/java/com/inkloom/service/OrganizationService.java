package com.inkloom.service;

import com.inkloom.dto.Dtos.*;
import com.inkloom.entity.*;
import com.inkloom.enums.OrgRole;
import com.inkloom.exception.ApiException;
import com.inkloom.repository.*;
import com.inkloom.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Organizations, teams, members, approval rules, audit trail. Every method checks the caller's role FIRST. */
@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository orgs;
    private final TeamRepository teams;
    private final OrganizationMemberRepository members;
    private final ApprovalRuleRepository rules;
    private final UserRepository users;
    private final AuditLogRepository auditRepo;
    private final ContentItemRepository contentRepo;
    private final AccessService access;
    private final AuditService audit;
    private final MapperService mapper;

    public OrganizationService(OrganizationRepository orgs, TeamRepository teams, OrganizationMemberRepository members,
                               ApprovalRuleRepository rules, UserRepository users, AuditLogRepository auditRepo,
                               ContentItemRepository contentRepo, AccessService access, AuditService audit, MapperService mapper) {
        this.orgs = orgs; this.teams = teams; this.members = members; this.rules = rules; this.users = users;
        this.auditRepo = auditRepo; this.contentRepo = contentRepo; this.access = access; this.audit = audit; this.mapper = mapper;
    }

    @Transactional
    public OrganizationResponse createOrganization(Long actorId, CreateOrganizationRequest req) {
        User actor = access.requireUser(actorId);
        String name = InputSanitizer.plain(req.name());
        if (name == null || name.length() < 2) throw ApiException.badRequest("Invalid organization name");
        if (orgs.existsByNameIgnoreCase(name)) throw ApiException.conflict("An organization with this name already exists");

        Organization org = orgs.save(new Organization(name, InputSanitizer.plain(req.description()), actor));
        members.save(new OrganizationMember(org, actor, OrgRole.ORG_ADMIN, null));     // creator becomes admin
        audit.write(actor, org.getId(), "ORG_CREATED", "Organization", org.getId(), "name=" + name);
        log.info("[ORG] created orgId={} name='{}' admin={}", org.getId(), name, actor.getEmail());
        return new OrganizationResponse(org.getId(), org.getName(), org.getDescription(), List.of(OrgRole.ORG_ADMIN));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> myOrganizations(Long actorId) {
        Map<Long, Organization> byId = new LinkedHashMap<>();
        Map<Long, List<OrgRole>> roles = new HashMap<>();
        for (OrganizationMember m : members.findByUserId(actorId)) {
            Long id = m.getOrganization().getId();
            byId.putIfAbsent(id, m.getOrganization());
            roles.computeIfAbsent(id, k -> new ArrayList<>()).add(m.getRole());
        }
        List<OrganizationResponse> out = new ArrayList<>();
        for (Organization o : byId.values()) out.add(new OrganizationResponse(o.getId(), o.getName(), o.getDescription(), roles.get(o.getId())));
        return out;
    }

    @Transactional
    public TeamResponse createTeam(Long actorId, Long orgId, CreateTeamRequest req) {
        User actor = access.requireUser(actorId);
        access.requireAnyRole(orgId, actorId, OrgRole.ORG_ADMIN);
        Organization org = requireOrg(orgId);
        String name = InputSanitizer.plain(req.name());
        if (name == null || name.length() < 2) throw ApiException.badRequest("Invalid team name");
        if (teams.existsByOrganizationIdAndNameIgnoreCase(orgId, name)) throw ApiException.conflict("Team already exists");
        Team t = teams.save(new Team(org, name));
        audit.write(actor, orgId, "TEAM_CREATED", "Team", t.getId(), "name=" + name);
        return new TeamResponse(t.getId(), t.getName());
    }

    @Transactional
    public MemberResponse addMember(Long actorId, Long orgId, AddMemberRequest req) {
        User actor = access.requireUser(actorId);
        access.requireAnyRole(orgId, actorId, OrgRole.ORG_ADMIN);                  // only admins manage employees
        Organization org = requireOrg(orgId);

        String email = req.email().trim().toLowerCase(Locale.ROOT);
        User target = users.findByEmail(email).orElseThrow(() -> ApiException.notFound("No registered user with that email. Ask them to sign up first."));
        if (members.existsByOrganizationIdAndUserIdAndRole(orgId, target.getId(), req.role())) {
            throw ApiException.conflict("User already has this role in the organization");
        }
        Team team = null;
        if (req.teamId() != null) {
            // team must belong to THIS organization (prevents cross-tenant references)
            team = teams.findByIdAndOrganizationId(req.teamId(), orgId).orElseThrow(() -> ApiException.badRequest("Team does not belong to this organization"));
        }
        OrganizationMember m = members.save(new OrganizationMember(org, target, req.role(), team));
        audit.write(actor, orgId, "MEMBER_ADDED", "OrganizationMember", m.getId(), target.getEmail() + " as " + req.role());
        log.info("[ORG] orgId={} added {} as {}", orgId, target.getEmail(), req.role());
        return toMember(m);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long actorId, Long orgId) {
        access.requireMember(orgId, actorId);
        List<MemberResponse> out = new ArrayList<>();
        for (OrganizationMember m : members.findByOrganizationId(orgId)) out.add(toMember(m));
        return out;
    }

    @Transactional
    public ApprovalRuleResponse upsertRule(Long actorId, Long orgId, ApprovalRuleRequest req) {
        User actor = access.requireUser(actorId);
        access.requireAnyRole(orgId, actorId, OrgRole.ORG_ADMIN);
        Organization org = requireOrg(orgId);
        if (!req.firstReviewerRole().isReviewerRole()) throw ApiException.badRequest("firstReviewerRole must be a reviewer role");
        if (req.finalApproverRole() != null && !req.finalApproverRole().isReviewerRole()) throw ApiException.badRequest("finalApproverRole must be a reviewer role");

        ApprovalRule rule = rules.findByOrganizationIdAndContentType(orgId, req.contentType()).orElse(null);
        if (rule == null) rule = new ApprovalRule(org, req.contentType(), req.firstReviewerRole(), req.finalApproverRole());
        else rule.change(req.firstReviewerRole(), req.finalApproverRole());
        rule = rules.save(rule);
        audit.write(actor, orgId, "RULE_SET", "ApprovalRule", rule.getId(),
                req.contentType() + ": " + req.firstReviewerRole() + " -> " + req.finalApproverRole());
        return toRule(rule);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRuleResponse> listRules(Long actorId, Long orgId) {
        access.requireMember(orgId, actorId);
        List<ApprovalRuleResponse> out = new ArrayList<>();
        for (ApprovalRule r : rules.findByOrganizationId(orgId)) out.add(toRule(r));
        return out;
    }

    /** The company review panel: all content aimed at this organization's products. */
    @Transactional(readOnly = true)
    public List<ContentResponse> orgContent(Long actorId, Long orgId) {
        if (!access.canViewOrgContent(orgId, actorId)) {
            log.warn("[ACCESS] DENIED userId={} cannot view org content org={}", actorId, orgId);
            throw ApiException.forbidden("Only admins and reviewers can see the organization's content");
        }
        List<ContentResponse> out = new ArrayList<>();
        for (ContentItem c : contentRepo.findByOrganization(orgId)) out.add(mapper.toContent(c));
        return out;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> auditLogs(Long actorId, Long orgId, int page, int size) {
        access.requireAnyRole(orgId, actorId, OrgRole.ORG_ADMIN);
        int safeSize = Math.min(Math.max(size, 1), 100);                        // cap page size
        int safePage = Math.max(page, 0);
        List<AuditLogResponse> out = new ArrayList<>();
        for (AuditLog a : auditRepo.findByOrgIdOrderByCreatedAtDesc(orgId, PageRequest.of(safePage, safeSize))) {
            out.add(new AuditLogResponse(a.getId(), a.getActorId(), a.getActorEmail(), a.getAction(), a.getEntityType(),
                    a.getEntityId(), a.getDetails(), a.getCreatedAt()));
        }
        return out;
    }

    private Organization requireOrg(Long orgId) {
        return orgs.findById(orgId).orElseThrow(() -> ApiException.notFound("Organization not found"));
    }

    private MemberResponse toMember(OrganizationMember m) {
        return new MemberResponse(m.getId(), m.getUser().getId(), m.getUser().getFullName(), m.getUser().getEmail(),
                m.getRole(), m.getTeam() == null ? null : m.getTeam().getName());
    }

    private ApprovalRuleResponse toRule(ApprovalRule r) {
        return new ApprovalRuleResponse(r.getId(), r.getContentType(), r.getFirstReviewerRole(), r.getFinalApproverRole());
    }
}
