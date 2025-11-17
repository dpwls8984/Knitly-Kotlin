package com.mysite.knitly.utility.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "custom.jwt")
data class JwtProperties(
    var secretKey: String = "",
    var accessTokenExpireSeconds: Long = 0,
    var refreshTokenExpireSeconds: Long = 0
)