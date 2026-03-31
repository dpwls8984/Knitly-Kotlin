package com.mysite.knitly.utility.config

import com.mysite.knitly.utility.handler.OAuth2FailureHandler
import com.mysite.knitly.utility.handler.OAuth2SuccessHandler
import com.mysite.knitly.utility.jwt.JwtAuthenticationFilter
import com.mysite.knitly.utility.oauth.CustomOAuth2UserService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity // --> 역할 : 스프링부트가 이 애너테이션을 보고 시큐리티 필터체인을 생성하고, DipatchServlet 앞쪽에 시큐리티 필터를 등록한다 => 이에 모든 요청이 시큐리티 필터를 거치게 된다
@EnableMethodSecurity // --> 역할 : 메서드 수준 권한을 활성화 : 서비스 계층에서 세밀한 접근 제어
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val oAuth2FailureHandler: OAuth2FailureHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    // 401/403을 JSON으로 내려주기 위한 핸들러
    private val jsonAuthEntryPoint: JsonAuthEntryPoint,
    private val jsonAccessDeniedHandler: JsonAccessDeniedHandler
) {

    /**
     * CORS 설정
     * 프론트엔드(localhost:3000)와 백엔드(localhost:8080) 간 통신 허용
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // 허용할 출처 (프론트엔드 URL)
            allowedOrigins = listOf(
                "http://localhost:3000",     // 개발 환경
                "http://localhost:3001",     // 개발 환경 (추가 포트)
                "https://www.myapp.com"      // 프로덕션 환경 (추후 변경)
            )

            // 허용할 HTTP 메서드
            allowedMethods = listOf(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
            )

            // 허용할 헤더
            allowedHeaders = listOf("*")

            // 쿠키 포함 허용 (매우 중요)
            allowCredentials = true

            // 노출할 헤더 (프론트엔드에서 접근 가능)
            exposedHeaders = listOf(
                "Authorization",
                "Set-Cookie"
            )

            // Preflight 요청 캐시 시간 (1시간)
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean // SecurityFilterChain을  Bean이 시큐리티 전체 규칙을 정의
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CORS 설정 적용
            .cors { it.configurationSource(corsConfigurationSource()) }

            // CSRF 비활성화 (JWT 사용)
            .csrf { it.disable() }

            // 세션 사용 안함 (Stateless)
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // 401/403 을 JSON 응답으로 고정
            .exceptionHandling {
                it.authenticationEntryPoint(jsonAuthEntryPoint)      // 401
                    .accessDeniedHandler(jsonAccessDeniedHandler)      // 403
            }

            // URL 별 권한 설정
            .authorizeHttpRequests { auth ->
                auth
                    // 프리플라이트(OPTIONS) 요청 허용
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // 커뮤니티 게시글 목록/상세 조회는 로그인 없이 허용
                    .requestMatchers(HttpMethod.GET, "/community/posts/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/community/comments/**").permitAll()
                    // 댓글 조회(게시글 하위 경로) 공개: 목록 & count 모두 포함
                    .requestMatchers(HttpMethod.GET, "/community/posts/*/comments").permitAll()
                    .requestMatchers(HttpMethod.GET, "/community/posts/*/comments/**").permitAll()

                    // 커뮤니티 "쓰기/수정/삭제"는 인증 필요
                    .requestMatchers(HttpMethod.POST, "/community/**").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/community/**").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/community/**").authenticated()
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
                    .requestMatchers("/reviews/**").permitAll()

                    // 업로드한 이미지 조회 (uploads 이미지 경로 허용)
                    .requestMatchers("/uploads/**").permitAll()

                    .requestMatchers(
                        "/resources/**",          // 정적 리소스
                        "/static/**",
                        "/files/**"               // 파일 접근
                    ).permitAll()

                    .requestMatchers(
                        "/api/public/**",
                        "/home/**",              // 홈 화면
                        "/products/**"           // 상품 목록 (읽기는 public)
                    ).permitAll()

                    .requestMatchers(
                        "/resources/**",          // 정적 리소스
                        "/static/**",
                        "/post/**", // 게시글 이미지
                        "/files/**"               // 파일 접근
                    ).permitAll()

                    // 나머지 모두 인증 필요
                    .anyRequest().authenticated()
            }

            // OAuth2 로그인 설정
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint {
                        it.userService(customOAuth2UserService)
                    }
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            }

            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

            // 인증 실패 시 401을 반환
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    // 🔥 401 반환 (리다이렉트 대신)
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = "application/json"
                    response.writer.write("{\"error\": \"Unauthorized\"}")
                }
            }

        return http.build()
    }
}