package com.inkloom.compliance;

import static org.junit.jupiter.api.Assertions.*;

import com.inkloom.compliance.engine.RuleEngine;
import com.inkloom.compliance.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();

    private final Rule banned = new Rule("1", "LEG-001", "No guarantees", "Don't promise.", "ALL",
            RuleType.BANNED_PHRASE, Severity.CRITICAL, List.of("guaranteed"), List.of(), "Soften it.", true);
    private final Rule disclaimer = new Rule("2", "LEG-002", "T&C line", "Needs terms.", "ALL",
            RuleType.REQUIRED_ANY, Severity.MAJOR, List.of("terms and conditions apply"), List.of(), "Add it.", true);

    @Test
    void flagsBannedWordAndMissingDisclaimer() {
        var r = engine.evaluate("Guaranteed results for everyone!", List.of(banned, disclaimer));
        assertEquals(2, r.findings().size());
        assertEquals(SuggestionSource.RULE_BASED, r.findings().get(0).suggestionSource());
        assertTrue(r.passed().isEmpty());
    }

    @Test
    void cleanScriptPasses() {
        var r = engine.evaluate("Designed to help you sleep. Terms and conditions apply.", List.of(banned, disclaimer));
        assertTrue(r.findings().isEmpty());
        assertEquals(2, r.passed().size());
    }
}
