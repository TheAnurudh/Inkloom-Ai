package com.inkloom.service;

import com.inkloom.entity.OrganizationMember;
import com.inkloom.entity.User;
import com.inkloom.enums.OrgRole;
import com.inkloom.exception.ApiException;
import com.inkloom.repository.OrganizationMemberRepository;
import com.inkloom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * THE authorization gate for organization data (tenant isolation).
 * Every service method that touches an organization MUST call one of these first.
 * DEBUG TIP: search the console for "[ACCESS]" to see why somebody was allowed or denied.
 */
@Service
public class AccessService {

    private static final Logger log = LoggerFactory.getLogger(AccessService.class);

    private final UserRepository users;
    private final OrganizationMemberRepository members;

    public AccessService(UserRepository users, OrganizationMemberRepository members) {
        this.users = users;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public Set<OrgRole> rolesOf(Long orgId, Long userId) {
        Set<OrgRole> roles = EnumSet.noneOf(OrgRole.class);
        for (OrganizationMember m : members.findByOrganizationIdAndUserId(orgId, userId)) roles.add(m.getRole());
        return roles;
    }

    public void requireMember(Long orgId, Long userId) {
        if (rolesOf(orgId, userId).isEmpty()) {
            log.warn("[ACCESS] DENIED userId={} is not a member of org={}", userId, orgId);
            throw ApiException.forbidden("You are not a member of this organization");
        }
    }

    public void requireAnyRole(Long orgId, Long userId, OrgRole... allowed) {
        Set<OrgRole> mine = rolesOf(orgId, userId);
        for (OrgRole r : allowed) {
            if (mine.contains(r)) {
                log.debug("[ACCESS] OK userId={} org={} has {}", userId, orgId, r);
                return;
            }
        }
        log.warn("[ACCESS] DENIED userId={} org={} has roles={} but needs one of {}", userId, orgId, mine, java.util.Arrays.toString(allowed));
        throw ApiException.forbidden("Your role does not allow this action");
    }

    /** True if the user may SEE the organization's content (admin, marketing manager or any reviewer). */
    public boolean canViewOrgContent(Long orgId, Long userId) {
        Set<OrgRole> mine = rolesOf(orgId, userId);
        if (mine.contains(OrgRole.ORG_ADMIN)) return true;
        for (OrgRole r : mine) if (r.isReviewerRole()) return true;
        return false;
    }
}
