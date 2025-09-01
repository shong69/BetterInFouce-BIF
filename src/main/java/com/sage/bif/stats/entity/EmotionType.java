package com.sage.bif.stats.entity;

import lombok.Getter;

@Getter
public enum EmotionType {

    ANGRY("화남", "😠", -2),
    DOWN("우울", "😢", -1),
    OKAY("평범", "😐", 0),
    GOOD("좋음", "😊", 1),
    GREAT("최고", "🤩", 2);

    private final String koreanName;
    private final String emoji;
    private final int score;

    EmotionType(final String koreanName, final String emoji, final int score) {
        this.koreanName = koreanName;
        this.emoji = emoji;
        this.score = score;
    }

    public boolean isPositive() {
        return score > 0;
    }

    public boolean isNegative() {
        return score < 0;
    }

    public boolean isNeutral() {
        return score == 0;
    }

    public static EmotionType fromScore(double score) {
        if (score >= 1.5) return GREAT;
        if (score >= 0.5) return GOOD;
        if (score >= -0.5) return OKAY;
        if (score >= -1.5) return DOWN;
        return ANGRY;
    }

}
