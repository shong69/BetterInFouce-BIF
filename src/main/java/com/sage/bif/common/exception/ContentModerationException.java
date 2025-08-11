package com.sage.bif.common.exception;

import com.sage.bif.common.client.ai.AzureOpenAiModerationClient.ModerationResult;
import lombok.Getter;

/**
 * 콘텐츠가 위험도 검사를 통과하지 못했을 때 발생하는 예외
 */
@Getter
public class ContentModerationException extends BaseException {
    
    private final ModerationResult moderationResult;
    
    public ContentModerationException(ModerationResult moderationResult) {
        super(ErrorCode.COMMON_AI_CONTENT_VIOLATION, 
            "콘텐츠가 위험도 검사를 통과하지 못했습니다. 위험도: " + getModerationSummary(moderationResult));
        this.moderationResult = moderationResult;
    }
    
    public ContentModerationException(ModerationResult moderationResult, String message) {
        super(ErrorCode.COMMON_AI_CONTENT_VIOLATION, message);
        this.moderationResult = moderationResult;
    }
    
    private static String getModerationSummary(ModerationResult result) {
        StringBuilder summary = new StringBuilder();
        
        if (result.isHate()) summary.append("혐오 ");
        if (result.isHateThreatening()) summary.append("혐오/위협 ");
        if (result.isSelfHarm()) summary.append("자해 ");
        if (result.isSexual()) summary.append("성적 ");
        if (result.isSexualMinors()) summary.append("성적/미성년자 ");
        if (result.isViolence()) summary.append("폭력 ");
        if (result.isViolenceGraphic()) summary.append("폭력/그래픽 ");
        
        return summary.length() > 0 ? summary.toString().trim() : "기타 위험 요소";
    }
}
