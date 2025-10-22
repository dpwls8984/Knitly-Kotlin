package com.mysite.knitly.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // 여기는 인터셉터/리소스핸들러/메시지컨버터 등 MVC 설정만 필요할 때 추가
    // 보안 관련 Bean, SecurityFilterChain, Jwt 필터, CORS Bean은 두지 않습니다.
}
