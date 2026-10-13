package com.lostvault;

import java.util.Set;

/** Reusable deterministic gate condition: every clue plus an optional gem threshold. */
public record UnlockCondition(Set<String> requiredClues, int minimumGems) {
    public UnlockCondition { requiredClues = Set.copyOf(requiredClues); if (minimumGems < 0) throw new IllegalArgumentException("minimumGems cannot be negative"); }
    public boolean isMet(Set<String> clues, int gems) { return gems >= minimumGems && clues.containsAll(requiredClues); }
}
