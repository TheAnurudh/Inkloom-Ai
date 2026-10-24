package com.inkloom.service;

import com.inkloom.entity.Product;
import com.inkloom.entity.RedFlag;
import com.inkloom.enums.ContentType;
import com.inkloom.enums.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * THE "AI" CHECKER (rule based, deterministic, works offline - perfect for a hackathon demo).
 *
 * It looks for: (1) risky marketing phrases, (2) phrases the company FORBIDS for this product,
 * (3) statistics that are not in the company's approved facts, (4) missing sponsorship disclosure.
 *
 * LATER: to use a real LLM, keep this method signature and call the model inside analyze().
 * Nothing else in the project needs to change.
 */
@Component
public class ClaimAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ClaimAnalyzer.class);
    private static final int CI = Pattern.CASE_INSENSITIVE;

    private record Rule(Pattern pattern, RiskLevel severity, String reason, String suggestion) { }

    private static final List<Rule> RULES = List.of(
        new Rule(Pattern.compile("\\b(100\\s*(%|percent)|hundred\\s+percent)", CI), RiskLevel.HIGH,
                "Absolute '100%' claims cannot be proven and can be treated as misleading advertising.",
                "Replace with a measured, evidence-backed statement (e.g. 'many users report...')."),
        new Rule(Pattern.compile("\\bguarantee(d|s)?\\b", CI), RiskLevel.HIGH,
                "A guarantee is a legal promise the company may not have made.",
                "Remove the guarantee or quote the company's official guarantee policy."),
        new Rule(Pattern.compile("\\b(cure[sd]?|curing)\\b", CI), RiskLevel.HIGH,
                "Medical 'cure' claims are regulated and almost always illegal for consumer products.",
                "Remove the medical claim. Describe only approved benefits."),
        new Rule(Pattern.compile("\\bno\\s+side\\s+effects?\\b|\\brisk[- ]free\\b|\\bzero\\s+risk\\b", CI), RiskLevel.HIGH,
                "Claiming zero risk / no side effects is unverifiable and can be harmful to buyers.",
                "Remove it, or add the official safety information."),
        new Rule(Pattern.compile("\\b(lose|burn|drop)\\s+\\d+\\s*(kg|kgs|lb|lbs|pounds)\\b", CI), RiskLevel.HIGH,
                "Specific weight-loss promises are health claims and are heavily regulated.",
                "Remove the number and avoid promising body results."),
        new Rule(Pattern.compile("best\\s+in\\s+the\\s+world|world'?s\\s+best|number\\s+(one|1)\\b|no\\.?\\s*1\\b|#1\\b", CI), RiskLevel.MEDIUM,
                "Superlatives ('best', 'number one') need independent proof.",
                "Use a personal opinion instead: 'my favourite' / 'I love it'."),
        new Rule(Pattern.compile("\\b(clinically\\s+proven|scientifically\\s+proven|doctor[- ]recommended|dermatologist[- ]approved)\\b", CI), RiskLevel.MEDIUM,
                "Endorsement/proof claims must be backed by a study or certificate.",
                "Only keep it if the company provides the study; otherwise remove it."),
        new Rule(Pattern.compile("\\b(overnight|instantly|in\\s+just\\s+\\d+\\s+(days?|hours?|minutes?))\\b", CI), RiskLevel.MEDIUM,
                "Unrealistic time-to-result promises mislead buyers.",
                "Say 'results vary' and avoid exact time promises.")
    );

    private static final Pattern DISCLOSURE = Pattern.compile(
            "#ad\\b|#sponsored|#partner|#collab|paid\\s+partnership|\\bsponsored\\b|in\\s+association\\s+with", CI);
    private static final Pattern PERCENT = Pattern.compile("\\d+(\\.\\d+)?\\s*%");

    public AnalysisResult analyze(Product product, ContentType type, String script) {
        long t0 = System.currentTimeMillis();
        List<RedFlag> flags = new ArrayList<>();
        String lower = script.toLowerCase(Locale.ROOT);

        // 1) generic risky phrases
        for (Rule r : RULES) {
            Matcher m = r.pattern().matcher(script);
            if (m.find()) {
                flags.add(new RedFlag("Found: \"" + snippet(m.group()) + "\"", r.reason(), r.suggestion(), r.severity()));
                log.debug("[AI] rule hit {} -> '{}'", r.severity(), snippet(m.group()));
            }
        }

        // 2) claims the company explicitly forbids for THIS product
        for (String forbidden : product.getForbiddenClaims()) {
            if (forbidden != null && !forbidden.isBlank() && lower.contains(forbidden.toLowerCase(Locale.ROOT))) {
                flags.add(new RedFlag("Forbidden claim: \"" + snippet(forbidden) + "\"",
                        product.getOrganization().getName() + " has explicitly forbidden this claim for " + product.getName() + ".",
                        "Delete this sentence. Use only approved claims.", RiskLevel.HIGH));
                log.debug("[AI] forbidden claim hit '{}'", snippet(forbidden));
            }
        }

        // 3) statistics not backed by the company's approved facts
        Matcher pm = PERCENT.matcher(script);
        int checked = 0;
        while (pm.find() && checked++ < 5) {
            String token = pm.group().replace(" ", "");
            if (token.startsWith("100")) continue;               // already covered by the 100% rule
            if (!approvedContains(product, token)) {
                flags.add(new RedFlag("Unverified statistic: \"" + token + "\"",
                        "This number is not in the company's approved facts for " + product.getName() + ".",
                        "Use only numbers from the approved claims list or remove the number.", RiskLevel.MEDIUM));
                log.debug("[AI] unverified statistic {}", token);
            }
        }

        // 4) sponsorship disclosure (not needed for pure banner designs)
        if (type != ContentType.BANNER_DESIGN && !DISCLOSURE.matcher(script).find()) {
            flags.add(new RedFlag("No sponsorship disclosure found",
                    "Paid/gifted promotions must be disclosed to the audience.",
                    "Add #ad or 'Paid partnership with " + product.getBrand() + "'.", RiskLevel.LOW));
        }

        log.debug("[AI] rules finished in {} ms, {} flag(s)", System.currentTimeMillis() - t0, flags.size());
        return score(product, flags, "RULES");
    }

    /** Turns a list of flags into score / level / summary. Public: the AI merge step reuses it. */
    public AnalysisResult score(Product product, List<RedFlag> flags, String source) {
        int score = 0;
        boolean anyHigh = false, anyMedium = false;
        for (RedFlag f : flags) {
            switch (f.getSeverity()) {
                case HIGH -> { score += 40; anyHigh = true; }
                case MEDIUM -> { score += 20; anyMedium = true; }
                default -> score += 5;
            }
        }
        score = Math.min(score, 100);
        RiskLevel level = (anyHigh || score >= 60) ? RiskLevel.HIGH : (anyMedium || score >= 20) ? RiskLevel.MEDIUM : RiskLevel.LOW;
        boolean meets = !anyHigh;

        String summary = flags.isEmpty()
                ? "No problems found. The script matches the known guidelines for " + product.getName() + "."
                : flags.size() + " issue(s) found. Risk: " + level + " (score " + score + "/100). "
                  + (meets ? "No blocking problems, but please review the flags."
                           : "Contains claims that can mislead buyers or create legal trouble.");
        log.debug("[AI] scored product={} source={} -> level={} score={} flags={}", product.getId(), source, level, score, flags.size());
        return new AnalysisResult(score, level, meets, summary, flags, source);
    }

    private boolean approvedContains(Product product, String token) {
        for (String claim : product.getApprovedClaims()) {
            if (claim != null && claim.replace(" ", "").contains(token)) return true;
        }
        return false;
    }

    private String snippet(String s) { return s.length() > 80 ? s.substring(0, 80) + "..." : s; }
}
