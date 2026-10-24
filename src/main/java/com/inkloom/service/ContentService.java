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

import java.time.Instant;
import java.util.*;

/**
 * Creator side of the workflow:
 *   analyze (instant check) -> create (auto AI check) -> update (re-check) -> submit (goes to company review panel)
 */
@Service
public class ContentService {

    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    private final ContentItemRepository contentRepo;
    private final ProductRepository productRepo;
    private final AiReportRepository reportRepo;
    private final ApprovalRuleRepository ruleRepo;
    private final ReviewTaskRepository taskRepo;
    private final AiAnalysisService analyzer;
    private final ReviewerAssigner assigner;
    private final AccessService access;
    private final AuditService audit;
    private final MapperService mapper;

    public ContentService(ContentItemRepository contentRepo, ProductRepository productRepo, AiReportRepository reportRepo,
                          ApprovalRuleRepository ruleRepo, ReviewTaskRepository taskRepo, AiAnalysisService analyzer,
                          ReviewerAssigner assigner, AccessService access, AuditService audit, MapperService mapper) {
        this.contentRepo = contentRepo; this.productRepo = productRepo; this.reportRepo = reportRepo;
        this.ruleRepo = ruleRepo; this.taskRepo = taskRepo; this.analyzer = analyzer; this.assigner = assigner;
        this.access = access; this.audit = audit; this.mapper = mapper;
    }

    /** INSTANT CHECK: analyses a script and returns the report. Nothing is stored. */
    @Transactional(readOnly = true)
    public AiReportResponse analyzeOnly(Long actorId, AnalyzeRequest req) {
        Product product = activeProduct(req.productId());
        AnalysisResult r = analyzer.analyze(actorId, product, req.contentType(), InputSanitizer.clean(req.script()));
        log.info("[CONTENT] instant check by userId={} product={} -> {} ({} flags)", actorId, product.getId(), r.level(), r.flags().size());
        return mapper.toReportFromResult(r, Instant.now());
    }

    @Transactional
    public ContentResponse create(Long actorId, CreateContentRequest req) {
        User actor = access.requireUser(actorId);
        Product product = activeProduct(req.productId());
        String script = InputSanitizer.clean(req.script());
        if (script == null || script.isBlank()) throw ApiException.badRequest("Script must not be empty");

        ContentItem item = new ContentItem(InputSanitizer.plain(req.title()), req.contentType(), script, actor, product);
        runAnalysis(item, product);
        audit.write(actor, product.getOrganization().getId(), "CONTENT_CREATED", "ContentItem", item.getId(),
                "risk=" + item.getRiskLevel());
        log.info("[CONTENT] created contentId={} by {} risk={}", item.getId(), actor.getEmail(), item.getRiskLevel());
        return mapper.toContent(item);
    }

    /** Creator edits the script (only before review or after changes were requested). AI re-checks automatically. */
    @Transactional
    public ContentResponse update(Long actorId, Long contentId, UpdateContentRequest req) {
        ContentItem item = ownedContent(actorId, contentId);
        if (item.getStatus() != ContentStatus.AI_CHECKED && item.getStatus() != ContentStatus.CHANGES_REQUESTED) {
            throw ApiException.conflict("Content cannot be edited while its status is " + item.getStatus());
        }
        String script = InputSanitizer.clean(req.script());
        if (script == null || script.isBlank()) throw ApiException.badRequest("Script must not be empty");
        item.updateContent(InputSanitizer.plain(req.title()), script);
        runAnalysis(item, item.getProduct());
        audit.write(item.getSubmittedBy(), item.getProduct().getOrganization().getId(), "CONTENT_UPDATED", "ContentItem",
                item.getId(), "risk=" + item.getRiskLevel());
        log.info("[CONTENT] updated contentId={} new risk={}", item.getId(), item.getRiskLevel());
        return mapper.toContent(item);
    }

    /** Sends the AI-checked content to the company's review panel (creates the first review task). */
    @Transactional
    public ContentResponse submit(Long actorId, Long contentId) {
        ContentItem item = ownedContent(actorId, contentId);
        if (item.getStatus() != ContentStatus.AI_CHECKED) {
            throw ApiException.conflict("Only AI-checked content can be submitted (current status: " + item.getStatus() + ")");
        }
        Long orgId = item.getProduct().getOrganization().getId();
        List<OrgRole> chain = buildChain(item, orgId);
        log.debug("[CONTENT] contentId={} approval chain = {}", item.getId(), chain);

        // fail early if any step has nobody who could review (so content never gets stuck)
        for (OrgRole role : chain) {
            if (assigner.pick(orgId, role, actorId, Set.of()).isEmpty()) {
                throw ApiException.conflict("The company has no available reviewer with role " + role + ". Please contact them.");
            }
        }

        item.startReview(chain);
        contentRepo.save(item);
        User reviewer = assigner.pick(orgId, chain.get(0), actorId, Set.of()).orElseThrow();
        ReviewTask task = taskRepo.save(new ReviewTask(item, item.getReviewRound(), 1, chain.get(0), reviewer));

        audit.write(item.getSubmittedBy(), orgId, "CONTENT_SUBMITTED", "ContentItem", item.getId(), "chain=" + chain);
        log.info("[CONTENT] contentId={} submitted. Task#{} step 1/{} assigned to {} ({})",
                item.getId(), task.getId(), chain.size(), reviewer.getEmail(), chain.get(0));
        return mapper.toContent(item);
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> mine(Long actorId) {
        List<ContentResponse> out = new ArrayList<>();
        for (ContentItem c : contentRepo.findBySubmittedByIdOrderByCreatedAtDesc(actorId)) out.add(mapper.toContent(c));
        return out;
    }

    /** Visible to the creator, and to the target company's admin/reviewers. Everyone else gets 404 (no information leak). */
    @Transactional(readOnly = true)
    public ContentResponse getById(Long actorId, Long contentId) {
        ContentItem item = contentRepo.findById(contentId).orElseThrow(() -> ApiException.notFound("Content not found"));
        boolean owner = item.getSubmittedBy().getId().equals(actorId);
        boolean orgStaff = access.canViewOrgContent(item.getProduct().getOrganization().getId(), actorId);
        if (!owner && !orgStaff) {
            log.warn("[ACCESS] DENIED userId={} tried to read contentId={} (not owner, not org staff)", actorId, contentId);
            throw ApiException.notFound("Content not found");
        }
        return mapper.toContent(item);
    }

    // ------------------------------------------------------------------ helpers

    private void runAnalysis(ContentItem item, Product product) {
        AnalysisResult r = analyzer.analyze(item.getSubmittedBy().getId(), product, item.getContentType(), item.getScript());
        item.applyAnalysis(r.level());
        contentRepo.save(item);
        reportRepo.save(new AiReport(item, r.score(), r.level(), r.meetsCriteria(), r.summary(), r.flags(), r.source()));
    }

    /** Org rule for this content type; a HIGH risk result always ends with the Legal Reviewer. */
    private List<OrgRole> buildChain(ContentItem item, Long orgId) {
        ApprovalRule rule = ruleRepo.findByOrganizationIdAndContentType(orgId, item.getContentType()).orElse(null);
        OrgRole first = rule != null ? rule.getFirstReviewerRole() : OrgRole.MARKETING_MANAGER;
        OrgRole last = rule != null ? rule.getFinalApproverRole() : OrgRole.BRAND_REVIEWER;
        if (item.getRiskLevel() == RiskLevel.HIGH) last = OrgRole.LEGAL_REVIEWER;
        List<OrgRole> chain = new ArrayList<>();
        chain.add(first);
        if (last != null && last != first) chain.add(last);
        return chain;
    }

    private Product activeProduct(Long productId) {
        return productRepo.findByIdAndActiveTrue(productId).orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    /** Loads content only if the caller is the creator. Anyone else -> 404. */
    private ContentItem ownedContent(Long actorId, Long contentId) {
        ContentItem item = contentRepo.findById(contentId).orElseThrow(() -> ApiException.notFound("Content not found"));
        if (!item.getSubmittedBy().getId().equals(actorId)) {
            log.warn("[ACCESS] DENIED userId={} tried to modify contentId={} owned by userId={}", actorId, contentId, item.getSubmittedBy().getId());
            throw ApiException.notFound("Content not found");
        }
        return item;
    }
}
