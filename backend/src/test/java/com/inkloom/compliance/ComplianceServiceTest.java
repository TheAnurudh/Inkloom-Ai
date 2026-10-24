package com.inkloom.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.inkloom.compliance.engine.RuleEngine;
import com.inkloom.compliance.model.ComplianceRequest;
import com.inkloom.compliance.model.Finding;
import com.inkloom.compliance.model.Flag;
import com.inkloom.compliance.model.Rule;
import com.inkloom.compliance.model.RuleType;
import com.inkloom.compliance.model.Severity;
import com.inkloom.compliance.model.SuggestionSource;
import com.inkloom.compliance.service.AiReviewService;
import com.inkloom.compliance.service.ComplianceService;
import com.inkloom.compliance.service.RuleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ComplianceServiceTest {

    @Test
    void reportKeepsOriginalScriptAndLeavesAiSuggestionsUnapplied() {
        String original = "  guaranteed results  ";
        Rule rule = new Rule("1", "LEG-001", "No guarantees", "Don't promise.", "ALL",
                RuleType.BANNED_PHRASE, Severity.CRITICAL, List.of("guaranteed"), List.of(),
                "Consider softer wording.", true);
        Finding aiFinding = new Finding("AI-REVIEW", "Consider the claim", SuggestionSource.AI,
                Flag.ORANGE, Severity.MINOR, "guaranteed", 2, 12,
                "This sounds like a promise.", "You could say it is designed to help.");

        RuleRepository repository = mock(RuleRepository.class);
        AiReviewService ai = mock(AiReviewService.class);
        when(repository.findByActiveTrue()).thenReturn(List.of(rule));
        when(ai.review(eq(original), anyList())).thenReturn(Optional.of(List.of(aiFinding)));

        ComplianceService service = new ComplianceService(repository, new RuleEngine(), ai);
        var report = service.check(new ComplianceRequest(original, "Demo", "General"));

        assertEquals(original, report.originalScript());
        assertFalse(report.suggestionsApplied());
        assertEquals(SuggestionSource.RULE_BASED, report.findings().get(0).suggestionSource());
        assertEquals(SuggestionSource.AI, report.findings().get(1).suggestionSource());
        assertEquals(2, report.findings().get(0).start());
    }
}
