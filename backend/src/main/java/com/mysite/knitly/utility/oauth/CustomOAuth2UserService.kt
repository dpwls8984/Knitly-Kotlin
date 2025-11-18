package com.mysite.knitly.utility.oauth

import kotlin.jvm.Throws;
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService : DefaultOAuth2UserService() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(OAuth2AuthenticationException::class)
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        // 1. Google로부터 사용자 정보 받아오기
        val oAuth2User = super.loadUser(userRequest)

        // 2. 어떤 OAuth 제공자인지 확인 (google)
        val registrationId = userRequest.clientRegistration.registrationId

        // 3. 사용자 정보 추출
        val attributes = oAuth2User.attributes
        val userInfo = OAuth2UserInfo.of(registrationId, attributes)

        // 로그로 확인
        log.info("[OAuth2] [Login] - Provider: {}", registrationId)
        log.info("[OAuth2] [Login] - Email: {}", userInfo.email)
        log.info("[OAuth2] [Login] - Name: {}", userInfo.name)

        // 4. OAuth2User 반환 (다음 단계 SuccessHandler로 전달됨)
        return oAuth2User
    }
}