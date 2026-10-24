package com.inkloom.service;

import com.inkloom.entity.OrganizationMember;
import com.inkloom.entity.User;
import com.inkloom.enums.OrgRole;
import com.inkloom.enums.TaskStatus;
import com.inkloom.repository.OrganizationMemberRepository;
import com.inkloom.repository.ReviewTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Picks who must review: right role, never the submitter, prefers new people, then the least busy. */
@Component
public class ReviewerAssigner {

    private static final Logger log = LoggerFactory.getLogger(ReviewerAssigner.class);

    private final OrganizationMemberRepository members;
    private final ReviewTaskRepository tasks;

    public ReviewerAssigner(OrganizationMemberRepository members, ReviewTaskRepository tasks) {
        this.members = members;
        this.tasks = tasks;
    }

    public Optional<User> pick(Long orgId, OrgRole role, Long submitterId, Set<Long> avoidUserIds) {
        List<User> candidates = new ArrayList<>();
        for (OrganizationMember m : members.findByOrganizationIdAndRole(orgId, role)) {
            User u = m.getUser();
            if (u.isEnabled() && !u.getId().equals(submitterId)) candidates.add(u);   // no self-approval
        }
        List<User> preferred = new ArrayList<>();
        for (User u : candidates) if (!avoidUserIds.contains(u.getId())) preferred.add(u);
        List<User> pool = preferred.isEmpty() ? candidates : preferred;

        Optional<User> chosen = pool.stream().min(
                Comparator.comparingLong((User u) -> tasks.countByReviewerIdAndStatus(u.getId(), TaskStatus.PENDING)));
        log.debug("[REVIEW] assigner org={} role={} candidates={} -> {}", orgId, role, candidates.size(),
                chosen.map(User::getEmail).orElse("NOBODY"));
        return chosen;
    }
}
