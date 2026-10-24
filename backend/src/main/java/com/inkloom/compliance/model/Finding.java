package com.inkloom.compliance.model;

/** One thing the checker noticed. start/end are character offsets so the UI can highlight (-1 = not tied to a spot). */
public record Finding(
        String ruleCode,
        String title,
        SuggestionSource suggestionSource,
        Flag flag,
        Severity severity,
        String matchedText,
        int start,
        int end,
        String message,
        String suggestion) {
}
