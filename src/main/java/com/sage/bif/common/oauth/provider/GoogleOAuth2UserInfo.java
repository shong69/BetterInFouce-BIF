package com.sage.bif.common.oauth.provider;

import com.sage.bif.common.oauth.OAuth2UserInfo;

import java.util.Map;

public record GoogleOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

}
