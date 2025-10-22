package com.mysite.knitly.utility.config;

import com.mysite.knitly.utility.handler.OAuth2FailureHandler;
import com.mysite.knitly.utility.handler.OAuth2SuccessHandler;
import com.mysite.knitly.utility.jwt.JwtAuthenticationFilter;
import com.mysite.knitly.utility.oauth.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 비밀번호 인코더
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager 주입 (필요 시)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // CORS 설정 (보안 설정 한 곳에만 둔다)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 필요에 따라 AllowedOrigins로 고정하거나, 개발 중이면 패턴 사용
        CorsConfiguration configuration = new CorsConfiguration();
        // 개발 중 전체 허용:
        configuration.setAllowedOriginPatterns(List.of("*"));
        // 또는 고정 도메인 방식:
        // configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000","http://localhost:3001","https://www.myapp.com"));

        configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization","Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS/CSRF/세션
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인가 규칙 (두 브랜치 규칙 + 커뮤니티 병합)
                .authorizeHttpRequests(auth -> auth
                        // 공개 GET API
                        .requestMatchers(HttpMethod.GET,
                                "/products", "/products/**",
                                "/users/*/products",
                                "/home/**",
                                "/community/**"
                        ).permitAll()

                        // 인증 불필요
                        .requestMatchers(
                                "/", "/login/**", "/oauth2/**",
                                "/auth/refresh", "/auth/test",        // dev 쪽
                                "/api/auth/refresh", "/api/auth/test" // feature 쪽
                        ).permitAll()

                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // 인증 필요
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/users/**").authenticated()

                        // 나머지
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                // JWT 필터
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
