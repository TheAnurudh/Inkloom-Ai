package com.inkloom.entity;

import com.inkloom.enums.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A script/caption/ad submitted by a creator for a company's product. */
@Entity
@Table(name = "content_items")
public class ContentItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150) private String title;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ContentType contentType;

    @Column(nullable = false, length = 5000) private String script;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentStatus status = ContentStatus.DRAFT;

    @Enumerated(EnumType.STRING) @Column(length = 10)
    private RiskLevel riskLevel;                         // from the latest AI report

    @ManyToOne(optional = false) @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private Product product;

    /** Frozen at submit time, e.g. "MARKETING_MANAGER,LEGAL_REVIEWER" (so later rule edits don't break running reviews). */
    @Column(length = 200) private String approvalChain;

    @Column(nullable = false) private int currentStep = 0;     // 1-based step inside approvalChain
    @Column(nullable = false) private int reviewRound = 0;     // +1 every time it is (re)submitted

    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    @Version private Long version;                           // optimistic locking (stops double updates)

    protected ContentItem() { }
    public ContentItem(String title, ContentType type, String script, User submittedBy, Product product) {
        this.title = title; this.contentType = type; this.script = script;
        this.submittedBy = submittedBy; this.product = product;
    }

    // ---- state changes (all state transitions live here so rules are in ONE place) ----
    public void applyAnalysis(RiskLevel level) { this.riskLevel = level; this.status = ContentStatus.AI_CHECKED; this.currentStep = 0; touch(); }
    public void updateContent(String newTitle, String newScript) { if (newTitle != null && !newTitle.isBlank()) this.title = newTitle; this.script = newScript; touch(); }
    public void startReview(List<OrgRole> chain) {
        StringBuilder sb = new StringBuilder();
        for (OrgRole r : chain) { if (sb.length() > 0) sb.append(','); sb.append(r.name()); }
        this.approvalChain = sb.toString(); this.currentStep = 1; this.reviewRound++;
        this.status = ContentStatus.PENDING_REVIEW; touch();
    }
    public void advanceStep() { this.currentStep++; touch(); }
    public void markApproved() { this.status = ContentStatus.APPROVED; touch(); }
    public void markRejected() { this.status = ContentStatus.REJECTED; touch(); }
    public void markChangesRequested() { this.status = ContentStatus.CHANGES_REQUESTED; touch(); }
    private void touch() { this.updatedAt = Instant.now(); }

    public List<OrgRole> getChain() {
        List<OrgRole> out = new ArrayList<>();
        if (approvalChain == null || approvalChain.isBlank()) return out;
        for (String s : approvalChain.split(",")) out.add(OrgRole.valueOf(s.trim()));
        return out;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public ContentType getContentType() { return contentType; }
    public String getScript() { return script; }
    public ContentStatus getStatus() { return status; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public User getSubmittedBy() { return submittedBy; }
    public Product getProduct() { return product; }
    public int getCurrentStep() { return currentStep; }
    public int getReviewRound() { return reviewRound; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
