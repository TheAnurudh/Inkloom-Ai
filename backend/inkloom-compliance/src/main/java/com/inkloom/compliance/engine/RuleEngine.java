package com.inkloom.compliance.engine;

import com.inkloom.compliance.model.Finding;
import com.inkloom.compliance.model.Rule;
import com.inkloom.compliance.model.SuggestionSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** The deterministic part: checks the script against each rule, same input = same answer. */
@Component
public class RuleEngine {

    private static final int MAX_HITS_PER_RULE = 5;

    public record Result(List<Finding> findings, List<String> passed) {}

    public Result evaluate(String text, List<Rule> rules) {
        List<Finding> findings = new ArrayList<>();
        List<String> passed = new ArrayList<>();

        for (Rule rule : rules) {
            List<Finding> hits = switch (rule.type()) {
                case BANNED_PHRASE -> findMatches(rule, text);
                case UNSUBSTANTIATED_CLAIM ->
                        containsAny(text, rule.evidenceHints()) ? List.of() : findMatches(rule, text);
                case REQUIRED_ANY ->
                        findMatches(rule, text).isEmpty() ? List.of(missing(rule)) : List.of();
            };
            if (hits.isEmpty()) passed.add(rule.title());
            else findings.addAll(hits);
        }
        return new Result(findings, passed);
    }

    private List<Finding> findMatches(Rule rule, String text) {
        List<Finding> out = new ArrayList<>();
        for (String raw : safe(rule.patterns())) {
            Matcher m = compile(raw).matcher(text);
            while (m.find() && out.size() < MAX_HITS_PER_RULE) {
                String hit = m.group();
                out.add(new Finding(
                        rule.code(), rule.title(), SuggestionSource.RULE_BASED,
                        rule.severity().flag(), rule.severity(),
                        hit, m.start(), m.end(),
                        "\u201C" + hit + "\u201D \u2014 " + rule.description(),
                        rule.suggestion()));
            }
        }
        return out;
    }

    private Finding missing(Rule rule) {
        return new Finding(
                rule.code(), rule.title(), SuggestionSource.RULE_BASED,
                rule.severity().flag(), rule.severity(),
                null, -1, -1,
                "Something's missing \u2014 " + rule.description(),
                rule.suggestion());
    }

    private boolean containsAny(String text, List<String> hints) {
        String lower = text.toLowerCase(Locale.ROOT);
        return safe(hints).stream().anyMatch(h -> lower.contains(h.toLowerCase(Locale.ROOT)));
    }

    private Pattern compile(String p) {
        if (p.startsWith("re:")) return Pattern.compile(p.substring(3));
        return Pattern.compile("(?<!\\w)" + Pattern.quote(p) + "(?!\\w)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static <T> List<T> safe(List<T> l) { return l == null ? List.of() : l; }
}
