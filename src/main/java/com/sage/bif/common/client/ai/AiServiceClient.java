package com.sage.bif.common.client.ai;

import com.sage.bif.common.client.ai.dto.AiChatSettings;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;

public interface AiServiceClient {

    AiResponse generate(AiRequest request);

    AiResponse generate(AiRequest request, AiChatSettings settings);

}
