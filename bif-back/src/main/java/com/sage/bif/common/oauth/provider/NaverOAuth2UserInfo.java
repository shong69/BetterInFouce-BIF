package com.sage.bif.common.oauth.provider;

import com.sage.bif.common.oauth.OAuth2UserInfo;

import java.util.Map;

public record NaverOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return response != null ? (String) response.get("email") : null;
    }

    @Override
    public String getId() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return response != null ? (String) response.get("id") : null;
    }

}
