package com.sage.bif.common.oauth;

import com.sage.bif.common.oauth.provider.GoogleOAuth2UserInfo;
import com.sage.bif.common.oauth.provider.KakaoOAuth2UserInfo;
import com.sage.bif.common.oauth.provider.NaverOAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo get(String registrationId, Map<String, Object> attributes) throws IllegalAccessException {

        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            default -> throw new IllegalAccessException("Unknown provider");
        };
    }
}
