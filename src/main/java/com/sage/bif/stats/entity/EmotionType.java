package com.sage.bif.stats.entity;

import lombok.Getter;

@Getter
public enum EmotionType {
    ANGRY("화남"),
    DOWN("우울"),
    OKAY("평범"),
    GOOD("좋음"),
    GREAT("최고");

    private final String koreanName;

    EmotionType(final String koreanName) {
        this.koreanName = koreanName;
    }
}