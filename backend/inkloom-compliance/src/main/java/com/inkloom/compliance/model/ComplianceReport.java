package com.inkloom.compliance.model;

import java.time.Instant;
import java.util.List;

public record ComplianceReport(
        String originalScript,
        String productName,
        int score,                 // 0-100, "how much of it is correct"
        Flag overallFlag,          // GREEN / ORANGE / RED
        String verdict,            // COMPLIANT / NEEDS_REVIEW / NON_COMPLIANT
        String summary,            // friendly one-paragraph explanation
        int redCount,
        int orangeCount,
        int greenCount,
        List<Finding> findings,    // everything that needs attention
        List<String> passedChecks, // everything that is fine
        boolean aiReviewUsed,
        boolean suggestionsApplied,
        Instant checkedAt) {
}
