package com.mysite.knitly.utility.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;


import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Google로부터 사용자 정보 받아오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 어떤 OAuth 제공자인지 확인 (google)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. 사용자 정보 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, attributes);

        // 로그로 확인
        log.info("OAuth2 Login - Provider: {}", registrationId);
        log.info("OAuth2 Login - Email: {}", userInfo.getEmail());
        log.info("OAuth2 Login - Name: {}", userInfo.getName());

        // 4. OAuth2User 반환 (다음 단계 SuccessHandler로 전달됨)
        return oAuth2User;
    }
}
