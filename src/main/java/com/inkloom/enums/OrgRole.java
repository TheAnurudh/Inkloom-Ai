package com.inkloom.enums;
/** Role of an employee INSIDE one organization. */
public enum OrgRole {
    ORG_ADMIN, CONTENT_CREATOR, MARKETING_MANAGER, DESIGN_REVIEWER, BRAND_REVIEWER, LEGAL_REVIEWER;

    /** Roles that are allowed to review/approve content. */
    public boolean isReviewerRole() {
        return this == MARKETING_MANAGER || this == DESIGN_REVIEWER
            || this == BRAND_REVIEWER || this == LEGAL_REVIEWER;
    }
}
