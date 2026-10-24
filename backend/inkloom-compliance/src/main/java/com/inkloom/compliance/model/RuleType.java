package com.inkloom.compliance.model;

public enum RuleType {
    /** Fails if any pattern appears (e.g. "guaranteed results"). */
    BANNED_PHRASE,
    /** Fails if a pattern appears but none of the evidence hints do (e.g. "clinically proven" with no source). */
    UNSUBSTANTIATED_CLAIM,
    /** Fails if NONE of the patterns appear (e.g. a mandatory disclaimer). */
    REQUIRED_ANY
}
