package com.mysite.knitly.utility.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "custom.jwt")
public class JwtProperties {

    private String secretKey;
    private long accessTokenExpireSeconds;
    private long refreshTokenExpireSeconds;
}
