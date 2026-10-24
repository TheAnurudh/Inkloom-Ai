package com.inkloom.service;

import com.inkloom.entity.RedFlag;
import com.inkloom.enums.RiskLevel;
import java.util.List;

/** Output of an analysis. source = RULES | AI+RULES | RULES_FALLBACK (AI was on but failed). */
public record AnalysisResult(int score, RiskLevel level, boolean meetsCriteria, String summary,
                             List<RedFlag> flags, String source) {

    public AnalysisResult withSource(String newSource) {
        return new AnalysisResult(score, level, meetsCriteria, summary, flags, newSource);
    }
}
