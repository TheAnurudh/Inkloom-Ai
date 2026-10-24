package com.inkloom.entity;

import com.inkloom.enums.ContentType;
import com.inkloom.enums.OrgRole;
import jakarta.persistence.*;

/** Per organization: for content type T the first reviewer is role A and the final approver is role B. */
@Entity
@Table(name = "approval_rules",
       uniqueConstraints = @UniqueConstraint(name = "uk_rule_org_type", columnNames = {"organization_id", "content_type"}))
public class ApprovalRule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING) @Column(name = "content_type", nullable = false, length = 40)
    private ContentType contentType;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private OrgRole firstReviewerRole;

    @Enumerated(EnumType.STRING) @Column(length = 30)
    private OrgRole finalApproverRole;              // null = single step approval

    protected ApprovalRule() { }
    public ApprovalRule(Organization organization, ContentType contentType, OrgRole first, OrgRole last) {
        this.organization = organization; this.contentType = contentType;
        this.firstReviewerRole = first; this.finalApproverRole = last;
    }

    public void change(OrgRole first, OrgRole last) { this.firstReviewerRole = first; this.finalApproverRole = last; }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public ContentType getContentType() { return contentType; }
    public OrgRole getFirstReviewerRole() { return firstReviewerRole; }
    public OrgRole getFinalApproverRole() { return finalApproverRole; }
}
