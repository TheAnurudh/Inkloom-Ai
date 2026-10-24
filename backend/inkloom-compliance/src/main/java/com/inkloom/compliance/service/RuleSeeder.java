package com.inkloom.compliance.service;

import com.inkloom.compliance.model.Rule;
import com.inkloom.compliance.model.RuleType;
import com.inkloom.compliance.model.Severity;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Starter rules so the checker works on day one. Replace these with the
 * company's real guidelines (or add them through POST /api/compliance/rules).
 */
@Component
public class RuleSeeder implements CommandLineRunner {

    private final RuleRepository repo;

    public RuleSeeder(RuleRepository repo) { this.repo = repo; }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        repo.saveAll(List.of(
            new Rule(null, "LEG-001", "No absolute guarantees",
                "We can't promise outcomes we can't control.", "ALL",
                RuleType.BANNED_PHRASE, Severity.CRITICAL,
                List.of("guaranteed", "100% guaranteed", "risk-free", "100% safe", "no side effects", "cure"),
                List.of(),
                "Soften it, e.g. \"designed to help\" or \"many customers see\" instead of a promise.", true),

            new Rule(null, "CLM-001", "Claims need proof",
                "Words like \"clinically proven\" must come with a source or certification.", "ALL",
                RuleType.UNSUBSTANTIATED_CLAIM, Severity.MAJOR,
                List.of("clinically proven", "scientifically proven", "doctor recommended", "award-winning", "proven to"),
                List.of("study", "according to", "source", "certified", "tested by", "reference", "report"),
                "Add where the claim comes from (a study, a certification, a named report) or drop the claim.", true),

            new Rule(null, "BRD-001", "No unverifiable superlatives",
                "\"Best\", \"number one\" and friends can't be verified unless we have a ranking to cite.", "ALL",
                RuleType.UNSUBSTANTIATED_CLAIM, Severity.MAJOR,
                List.of("best in the world", "world's best", "number one", "#1", "no.1", "unbeatable", "unmatched"),
                List.of("ranked", "according to", "survey", "rated by"),
                "Say what makes it stand out (\"loved for its battery life\") rather than crowning it the best.", true),

            new Rule(null, "BRD-002", "Don't attack competitors",
                "Comparisons with competitors need legal sign-off.", "ALL",
                RuleType.BANNED_PHRASE, Severity.MAJOR,
                List.of("better than", "unlike our competitors", "beats everyone", "worse than"),
                List.of(),
                "Focus on what our product does well instead of comparing against others.", true),

            new Rule(null, "LEG-002", "Terms & conditions line",
                "Promotional copy must point people to the terms.", "ALL",
                RuleType.REQUIRED_ANY, Severity.MAJOR,
                List.of("terms and conditions apply", "t&c apply", "terms apply", "conditions apply"),
                List.of(),
                "Add a short line at the end, like \"*Terms and conditions apply.\"", true),

            new Rule(null, "TON-001", "Keep the tone calm",
                "No shouting: ALL-CAPS words and stacked exclamation marks hurt brand voice.", "ALL",
                RuleType.BANNED_PHRASE, Severity.MINOR,
                List.of("re:\\b[A-Z]{5,}\\b", "re:!{2,}"),
                List.of(),
                "Use normal capitalization and a single exclamation mark at most.", true)
        ));
    }
}
