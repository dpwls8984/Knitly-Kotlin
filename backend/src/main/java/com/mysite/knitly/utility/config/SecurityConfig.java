package com.mysite.knitly.utility.config;

import com.mysite.knitly.utility.handler.OAuth2FailureHandler;
import com.mysite.knitly.utility.handler.OAuth2SuccessHandler;
import com.mysite.knitly.utility.jwt.JwtAuthenticationFilter;
import com.mysite.knitly.utility.oauth.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.mysite.knitly.utility.config.JsonAuthEntryPoint;        // 401 JSON
import com.mysite.knitly.utility.config.JsonAccessDeniedHandler;   // 403 JSON

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    // 401/403을 JSON으로 내려주기 위한 핸들러
    private final JsonAuthEntryPoint jsonAuthEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    /**
     * CORS 설정
     * 프론트엔드(localhost:3000)와 백엔드(localhost:8080) 간 통신 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 🔥 허용할 출처 (프론트엔드 URL)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",     // 개발 환경
                "http://localhost:3001",     // 개발 환경 (추가 포트)
                "https://www.myapp.com"      // 프로덕션 환경 (추후 변경)
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 🔥 쿠키 포함 허용 (매우 중요!)
        configuration.setAllowCredentials(true);

        // 노출할 헤더 (프론트엔드에서 접근 가능)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie"
        ));

        // Preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용)
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안함 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 401/403 을 JSON 응답으로 고정
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jsonAuthEntryPoint)      // 401
                        .accessDeniedHandler(jsonAccessDeniedHandler)      // 403
                )

                // URL 별 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 커뮤니티 게시글 목록/상세 조회는 로그인 없이 허용
                        .requestMatchers(HttpMethod.GET, "/community/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/community/comments/**").permitAll()
                        // 댓글 조회(게시글 하위 경로) 공개: 목록 & count 모두 포함
                        .requestMatchers(HttpMethod.GET, "/community/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/community/posts/*/comments/**").permitAll()

                        // 커뮤니티 "쓰기/수정/삭제"는 인증 필요
                        .requestMatchers(HttpMethod.POST,   "/community/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/community/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/community/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/community/**").authenticated()

                        // 마이페이지는 전부 인증 필요
                        .requestMatchers("/mypage/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/products", "/products/**", "/users/*/products").permitAll() // 상품 목록 API 공개
                        .requestMatchers(HttpMethod.GET, "/home/**").permitAll() // 홈 화면 API 공개
                        // 인증 불필요
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/auth/refresh", "/auth/test").permitAll()

                        // JWT 인증 필요
                        .requestMatchers("/users/**").authenticated()

                        // Swagger 사용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // 업로드한 리뷰 이미지 조회
                        .requestMatchers("/review/**").permitAll()

                        // 나머지 모두 인증 필요
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}