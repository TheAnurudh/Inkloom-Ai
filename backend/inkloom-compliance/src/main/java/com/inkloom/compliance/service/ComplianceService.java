package com.inkloom.compliance.service;

import com.inkloom.compliance.engine.RuleEngine;
import com.inkloom.compliance.model.ComplianceReport;
import com.inkloom.compliance.model.ComplianceRequest;
import com.inkloom.compliance.model.Finding;
import com.inkloom.compliance.model.Flag;
import com.inkloom.compliance.model.Rule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ComplianceService {

    private final RuleRepository rules;
    private final RuleEngine engine;
    private final AiReviewService ai;

    public ComplianceService(RuleRepository rules, RuleEngine engine, AiReviewService ai) {
        this.rules = rules;
        this.engine = engine;
        this.ai = ai;
    }

    public ComplianceReport check(ComplianceRequest req) {
        String text = req.content();

        List<Rule> active = rules.findByActiveTrue().stream()
                .filter(r -> appliesTo(r, req.category()))
                .toList();

        RuleEngine.Result result = engine.evaluate(text, active);
        List<Finding> findings = new ArrayList<>(result.findings());

        Optional<List<Finding>> aiFindings = ai.review(text, active);
        aiFindings.ifPresent(findings::addAll);

        int score = score(findings);
        Flag overall = overallFlag(findings, score);
        int checksRun = active.size();

        findings.sort((a, b) -> {
            int byFlag = b.flag().compareTo(a.flag());      // RED first
            return byFlag != 0 ? byFlag : Integer.compare(a.start(), b.start());
        });

        return new ComplianceReport(
                req.content(), req.productName(), score, overall, verdict(overall),
                HumanSummary.write(overall, score, checksRun, findings),
                count(findings, Flag.RED), count(findings, Flag.ORANGE), result.passed().size(),
                findings, result.passed(), aiFindings.isPresent(), false, Instant.now());
    }

    public List<Rule> listRules() { return rules.findAll(); }

    public Rule addRule(Rule rule) { return rules.save(rule); }

    /** Each distinct problem costs points once, however many times it appears, so one repeated word isn't fatal. */
    private int score(List<Finding> findings) {
        Map<String, Integer> penalties = new HashMap<>();
        for (Finding f : findings) {
            penalties.merge(f.ruleCode() + "|" + f.title(), f.severity().penalty(), Math::max);
        }
        int total = penalties.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(0, 100 - total);
    }

    private Flag overallFlag(List<Finding> findings, int score) {
        if (score < 50 || findings.stream().anyMatch(f -> f.flag() == Flag.RED)) return Flag.RED;
        if (!findings.isEmpty()) return Flag.ORANGE;
        return Flag.GREEN;
    }

    private String verdict(Flag flag) {
        return switch (flag) {
            case GREEN -> "COMPLIANT";
            case ORANGE -> "NEEDS_REVIEW";
            case RED -> "NON_COMPLIANT";
        };
    }

    private int count(List<Finding> list, Flag flag) {
        return (int) list.stream().filter(f -> f.flag() == flag).count();
    }

    private boolean appliesTo(Rule r, String category) {
        return r.category() == null || r.category().equalsIgnoreCase("ALL")
                || (category != null && r.category().equalsIgnoreCase(category));
    }
}
