package com.inkloom.compliance.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One company rule. Patterns are plain phrases (case-insensitive, whole-word)
 * unless they start with "re:", which means a raw regex (case-sensitive).
 * category = "ALL" applies the rule to every product.
 */
@Document("compliance_rules")
public record Rule(
        @Id String id,
        String code,
        String title,
        String description,
        String category,
        RuleType type,
        Severity severity,
        List<String> patterns,
        List<String> evidenceHints,
        String suggestion,
        boolean active) {
}
