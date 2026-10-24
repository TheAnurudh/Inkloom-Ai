package com.inkloom.entity;

import com.inkloom.enums.RiskLevel;
import jakarta.persistence.*;

/** One problem found by the AI inside a script. Stored inside the AiReport. */
@Embeddable
public class RedFlag {

    @Column(length = 500) private String issue;
    @Column(length = 500) private String reason;
    @Column(length = 500) private String suggestedAction;
    @Enumerated(EnumType.STRING) @Column(length = 10) private RiskLevel severity;

    protected RedFlag() { }
    public RedFlag(String issue, String reason, String suggestedAction, RiskLevel severity) {
        this.issue = cut(issue); this.reason = cut(reason); this.suggestedAction = cut(suggestedAction); this.severity = severity;
    }
    private static String cut(String s) { return s != null && s.length() > 500 ? s.substring(0, 500) : s; }

    public String getIssue() { return issue; }
    public String getReason() { return reason; }
    public String getSuggestedAction() { return suggestedAction; }
    public RiskLevel getSeverity() { return severity; }
}
