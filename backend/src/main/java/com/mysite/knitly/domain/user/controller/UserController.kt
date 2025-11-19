package com.mysite.knitly.domain.user.controller

import com.mysite.knitly.domain.product.product.dto.ProductListResponse
import com.mysite.knitly.domain.product.product.service.ProductService
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.service.UserService
import com.mysite.knitly.utility.auth.service.AuthService
import com.mysite.knitly.utility.cookie.CookieUtil
import com.mysite.knitly.utility.jwt.JwtProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/users")
class UserController(
    private val authService: AuthService,
    private val productService: ProductService,
    private val cookieUtil: CookieUtil,
    private val userService: UserService,
    private val jwtProvider: JwtProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
    }

    /**
     * 현재 로그인한 사용자 정보 조회 (JWT 인증 필요)
     * GET /users/me
     */
    @Operation(
        summary = "내 정보 조회",
        description = "현재 로그인한 사용자의 정보를 조회합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(
                mediaType = "application/json",
                examples = [ExampleObject(
                    value = """
                    {
                        "userId": "1",
                        "email": "user@example.com",
                        "name": "홍길동",
                        "provider": "GOOGLE",
                        "createdAt": "2025-01-20T15:52:58"
                    }
                    """
                )]
            )]
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 토큰 없음 또는 만료"
        )
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<Map<String, Any>> {
        if (user == null) {
            log.warn("[Auth] [UserController] User is null in /api/user/me")
            return ResponseEntity.status(401).build()
        }

        log.info("[Auth] [UserController] User info requested - userId: {}", user.userId)

        val response: Map<String, Any> = mapOf(
            "userId" to user.userId,
            "email" to user.email,
            "name" to user.name,
            "provider" to user.provider,
            "createdAt" to user.createdAt!!
        )

        return ResponseEntity.ok(response)
    }

    /**
     * 로그아웃 (개선됨)
     * POST /users/logout
     *
     * Redis에서 Refresh Token 삭제 + HTTP-only 쿠키 삭제
     */
    @Operation(
        summary = "로그아웃",
        description = "로그아웃하고 Redis에 저장된 Refresh Token과 HTTP-only 쿠키를 삭제합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = [Content(
                mediaType = "text/plain",
                examples = [ExampleObject(value = "로그아웃되었습니다.")]
            )]
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패"
        )
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    fun logout(
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<String> {

        // 🔥 AT에서 userId 추출
        val userId = authHeader
            ?.takeIf { it.startsWith("Bearer ") }
            ?.let {
                try {
                    val token = it.substring(7)
                    jwtProvider.getUserIdFromToken(token).also { id ->
                        log.info("[Auth] [UserController] Extracted userId from access token: {}", id)
                    }
                } catch (e: Exception) {
                    log.warn("[Auth] [UserController] Failed to extract userId from access token", e)
                    null
                }
            }

        log.info("[Auth] [UserController] Logout requested - userId: {}", userId)

        // 1. Redis에서 Refresh Token 삭제
        if (userId != null) {
            authService.logout(userId)
            log.info("[Auth] [UserController] Refresh Token deleted from Redis - userId: {}", userId)
        } else {
            log.info("[Auth] [UserController] Skipping Redis deletion - userId is null")
        }

        // 2. HTTP-only 쿠키 삭제
        cookieUtil.deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
        log.info("[Auth] [UserController] Refresh Token cookie deleted")

        return ResponseEntity.ok("[Auth] [UserController] 로그아웃되었습니다.")
    }

    /**
     * 회원탈퇴 (개선됨)
     * DELETE /users/me
     *
     * DB에서 사용자 정보 삭제 + Redis에서 Refresh Token 삭제 + HTTP-only 쿠키 삭제
     */
    @Operation(
        summary = "회원탈퇴",
        description = "회원탈퇴하고 DB와 Redis에서 모든 사용자 데이터를 삭제하며, HTTP-only 쿠키도 삭제합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "탈퇴 성공",
            content = [Content(
                mediaType = "text/plain",
                examples = [ExampleObject(value = "회원탈퇴가 완료되었습니다.")]
            )]
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패"
        )
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/me")
    fun deleteAccount(
        @AuthenticationPrincipal user: User?,
        response: HttpServletResponse
    ): ResponseEntity<String> {

        if (user == null) {
            log.warn("[Auth] [UserController] User is null in DELETE /user/me")
            return ResponseEntity.status(401).body("[Auth] [UserController] 인증이 필요합니다.")
        }

        log.info("[Auth] [UserController] Account deletion requested - userId: {}, email: {}", user.userId, user.email)

        // 1. DB에서 사용자 삭제 + Redis에서 Refresh Token 삭제
        authService.deleteAccount(user.userId)
        log.info("[Auth] [UserController] User account deleted from DB and Redis - userId: {}", user.userId)

        // 2. HTTP-only 쿠키 삭제
        cookieUtil.deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
        log.info("[Auth] [UserController] Refresh Token cookie deleted")

        return ResponseEntity.ok("회원탈퇴가 완료되었습니다.")
    }

    /**
     * 유저가 판매하는 상품 조회 (AT 불필요)
     * GET users/{userId}/products
     */
    @Operation(
        summary = "판매자 상품 조회",
        description = "해당 유저가 판매중인 상품 목록을 가져옵니다."
    )
    @GetMapping("/{userId}/products")
    fun getProductsWithUserId(
        @PathVariable userId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<ProductListResponse>> {
        val response = productService.findProductsByUserId(userId, pageable)
        log.info("getProductsWithUserId response: {}", response)
        return ResponseEntity.ok(response) as ResponseEntity<Page<ProductListResponse>>
    }
}