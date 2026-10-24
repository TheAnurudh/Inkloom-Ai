package com.inkloom.service;

import com.inkloom.dto.Dtos.*;
import com.inkloom.entity.*;
import com.inkloom.enums.*;
import com.inkloom.exception.ApiException;
import com.inkloom.repository.*;
import com.inkloom.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Company side of the workflow. The decide() method is the most security-sensitive code of the project,
 * read its numbered checks. Each one blocks a different kind of misuse.
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewTaskRepository taskRepo;
    private final ReviewDecisionRepository decisionRepo;
    private final ContentItemRepository contentRepo;
    private final OrganizationMemberRepository memberRepo;
    private final ReviewerAssigner assigner;
    private final AccessService access;
    private final AuditService audit;
    private final MapperService mapper;

    public ReviewService(ReviewTaskRepository taskRepo, ReviewDecisionRepository decisionRepo, ContentItemRepository contentRepo,
                         OrganizationMemberRepository memberRepo, ReviewerAssigner assigner, AccessService access,
                         AuditService audit, MapperService mapper) {
        this.taskRepo = taskRepo; this.decisionRepo = decisionRepo; this.contentRepo = contentRepo;
        this.memberRepo = memberRepo; this.assigner = assigner; this.access = access; this.audit = audit; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ReviewTaskResponse> myTasks(Long actorId, TaskStatus status) {
        List<ReviewTaskResponse> out = new ArrayList<>();
        for (ReviewTask t : taskRepo.findByReviewerIdAndStatusOrderByCreatedAtAsc(actorId, status)) out.add(mapper.toTask(t));
        log.debug("[REVIEW] userId={} has {} {} task(s)", actorId, out.size(), status);
        return out;
    }

    @Transactional(readOnly = true)
    public ReviewTaskResponse getTask(Long actorId, Long taskId) {
        ReviewTask t = taskRepo.findById(taskId).orElseThrow(() -> ApiException.notFound("Review task not found"));
        if (!t.getReviewer().getId().equals(actorId)) {
            log.warn("[ACCESS] DENIED userId={} tried to read task={} assigned to userId={}", actorId, taskId, t.getReviewer().getId());
            throw ApiException.notFound("Review task not found");       // 404, not 403: do not reveal that it exists
        }
        return mapper.toTask(t);
    }

    @Transactional
    public DecisionResult decide(Long actorId, Long taskId, ReviewDecisionRequest req) {
        User actor = access.requireUser(actorId);
        ReviewTask task = taskRepo.findById(taskId).orElseThrow(() -> ApiException.notFound("Review task not found"));
        ContentItem item = task.getContentItem();
        Long orgId = item.getProduct().getOrganization().getId();
        log.debug("[REVIEW] decide: user={} task={} step={} role={} decision={}", actor.getEmail(), taskId, task.getStep(),
                task.getRequiredRole(), req.decision());

        // CHECK 1: only the assigned reviewer can decide (blocks other employees and outsiders)
        if (!task.getReviewer().getId().equals(actorId)) {
            log.warn("[ACCESS] DENIED check1 userId={} is not the assigned reviewer of task={}", actorId, taskId);
            throw ApiException.notFound("Review task not found");
        }
        // CHECK 2: task still open (blocks double decisions / replay)
        if (task.getStatus() != TaskStatus.PENDING) throw ApiException.conflict("This task was already completed");
        // CHECK 3: content is really waiting for review, and this is the CURRENT step (blocks out-of-order approvals)
        if (item.getStatus() != ContentStatus.PENDING_REVIEW || item.getCurrentStep() != task.getStep()) {
            throw ApiException.conflict("This content is not waiting for this review step");
        }
        // CHECK 4: reviewer still holds the required role in that organization (role may have been removed since)
        if (!memberRepo.existsByOrganizationIdAndUserIdAndRole(orgId, actorId, task.getRequiredRole())) {
            log.warn("[ACCESS] DENIED check4 userId={} no longer has role {} in org={}", actorId, task.getRequiredRole(), orgId);
            throw ApiException.forbidden("You no longer have the role required for this review");
        }
        // CHECK 5: no self-approval
        if (item.getSubmittedBy().getId().equals(actorId)) {
            log.warn("[ACCESS] DENIED check5 userId={} tried to review own content", actorId);
            throw ApiException.forbidden("You cannot review your own content");
        }
        // CHECK 6: reject / change request must explain why
        String comment = InputSanitizer.plain(req.comment());
        if (req.decision() != Decision.APPROVE && (comment == null || comment.isBlank())) {
            throw ApiException.badRequest("A comment is required when rejecting or requesting changes");
        }

        decisionRepo.save(new ReviewDecision(task, req.decision(), comment, actor));
        task.complete();
        taskRepo.save(task);

        String message;
        switch (req.decision()) {
            case APPROVE -> {
                List<OrgRole> chain = item.getChain();
                if (task.getStep() < chain.size()) {
                    OrgRole nextRole = chain.get(task.getStep());             // step is 1-based, so index = step
                    Set<Long> previous = new HashSet<>();
                    for (ReviewTask t : taskRepo.findByContentItemIdOrderByStepAsc(item.getId())) {
                        if (t.getReviewRound() == item.getReviewRound()) previous.add(t.getReviewer().getId());
                    }
                    User next = assigner.pick(orgId, nextRole, item.getSubmittedBy().getId(), previous)
                            .orElseThrow(() -> ApiException.conflict("No available reviewer with role " + nextRole));
                    item.advanceStep();
                    contentRepo.save(item);
                    taskRepo.save(new ReviewTask(item, item.getReviewRound(), item.getCurrentStep(), nextRole, next));
                    message = "Approved. Sent to " + nextRole + " (" + next.getFullName() + ") for step " + item.getCurrentStep() + "/" + chain.size();
                } else {
                    item.markApproved();
                    contentRepo.save(item);
                    message = "Final approval done. Content is APPROVED.";
                }
            }
            case REJECT -> { item.markRejected(); contentRepo.save(item); message = "Content REJECTED."; }
            default -> { item.markChangesRequested(); contentRepo.save(item); message = "Changes requested. Creator can edit and resubmit."; }
        }

        audit.write(actor, orgId, "REVIEW_" + req.decision(), "ReviewTask", task.getId(),
                "content=" + item.getId() + " step=" + task.getStep() + " comment=" + comment);
        log.info("[REVIEW] task={} {} by {} -> content {} is now {}", taskId, req.decision(), actor.getEmail(), item.getId(), item.getStatus());
        return new DecisionResult(task.getId(), req.decision(), item.getStatus(), message);
    }
}
