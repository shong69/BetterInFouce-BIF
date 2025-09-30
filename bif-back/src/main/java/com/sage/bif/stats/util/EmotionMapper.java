package com.sage.bif.stats.util;

import com.sage.bif.diary.model.Emotion;
import com.sage.bif.stats.entity.EmotionType;
import lombok.extern.slf4j.Slf4j;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
public class EmotionMapper {

    private EmotionMapper() {
    }

    private static final Map<Emotion, EmotionType> DIARY_TO_STATS_MAP = new EnumMap<>(Emotion.class);

    static {
        DIARY_TO_STATS_MAP.put(Emotion.EXCELLENT, EmotionType.GREAT);
        DIARY_TO_STATS_MAP.put(Emotion.JOY, EmotionType.GOOD);
        DIARY_TO_STATS_MAP.put(Emotion.NEUTRAL, EmotionType.OKAY);
        DIARY_TO_STATS_MAP.put(Emotion.SAD, EmotionType.DOWN);
        DIARY_TO_STATS_MAP.put(Emotion.ANGER, EmotionType.ANGRY);
    }

    public static EmotionType mapDiaryEmotionToStats(final Emotion diaryEmotion) {
        final EmotionType statsEmotion = DIARY_TO_STATS_MAP.get(diaryEmotion);
        if (statsEmotion == null) {
            log.warn("Unknown diary emotion: {}, mapping to NEUTRAL", diaryEmotion);
            return EmotionType.OKAY;
        }
        return statsEmotion;
    }

}
