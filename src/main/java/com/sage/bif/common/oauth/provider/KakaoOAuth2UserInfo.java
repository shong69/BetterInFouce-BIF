package com.sage.bif.common.oauth.provider;

import com.sage.bif.common.oauth.OAuth2UserInfo;

import java.util.Map;

public record KakaoOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account != null ? (String) account.get("email") : null;
    }

    @Override
    public String getId() {
        return attributes.get("id").toString();
    }

}
