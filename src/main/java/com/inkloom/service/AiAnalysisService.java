package com.inkloom.service;

import com.inkloom.entity.Product;
import com.inkloom.entity.RedFlag;
import com.inkloom.enums.ContentType;
import com.inkloom.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the analysis:  rules (always)  +  real AI (if enabled and working)  =  merged report.
 * If the AI is off or fails, the rule-based result is returned with source RULES / RULES_FALLBACK.
 * Also limits how many checks one user may run per hour (protects your AI bill).
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final ClaimAnalyzer rules;
    private final LlmClaimChecker llm;
    private final int maxPerHour;
    private final Map<Long, Deque<Long>> usage = new ConcurrentHashMap<>();

    public AiAnalysisService(ClaimAnalyzer rules, LlmClaimChecker llm,
                             @Value("${ai.max-checks-per-user-per-hour:30}") int maxPerHour) {
        this.rules = rules; this.llm = llm; this.maxPerHour = maxPerHour;
    }

    public AnalysisResult analyze(Long actorId, Product product, ContentType type, String script) {
        checkQuota(actorId);
        AnalysisResult base = rules.analyze(product, type, script);

        Optional<List<RedFlag>> extra = llm.findFlags(product, type, script);
        if (extra.isEmpty()) {
            return llm.isEnabled() ? base.withSource("RULES_FALLBACK") : base;
        }
        List<RedFlag> merged = new ArrayList<>(base.flags());
        Set<String> seen = new HashSet<>();
        for (RedFlag f : merged) seen.add(f.getIssue().toLowerCase(Locale.ROOT));
        for (RedFlag f : extra.get()) {
            if (seen.add(f.getIssue().toLowerCase(Locale.ROOT))) merged.add(f);
        }
        AnalysisResult result = rules.score(product, merged, "AI+RULES");
        log.debug("[AI] merged: {} rule flag(s) + {} AI flag(s) -> {}", base.flags().size(), extra.get().size(), result.level());
        return result;
    }

    private void checkQuota(Long actorId) {
        long now = System.currentTimeMillis();
        Deque<Long> q = usage.computeIfAbsent(actorId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > 3_600_000L) q.pollFirst();
            if (q.size() >= maxPerHour) {
                log.warn("[AI] quota exceeded for userId={}", actorId);
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Hourly limit of " + maxPerHour + " AI checks reached. Try again later.");
            }
            q.addLast(now);
        }
    }
}
