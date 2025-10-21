package com.mysite.knitly.domain.user.controller;

import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.service.ProductService;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.utility.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "User", description = "사용자 관리 API")
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final ProductService productService;

    /**
     * 현재 로그인한 사용자 정보 조회 (JWT 인증 필요)
     * GET /users/me
     */
    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 정보를 조회합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                        "userId": "1",
                        "email": "user@example.com",
                        "name": "홍길동",
                        "provider": "GOOGLE",
                        "createdAt": "2025-01-20T15:52:58"
                    }
                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 만료")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {

        if (user == null) {
            log.warn("User is null in /api/user/me");
            return ResponseEntity.status(401).build();
        }

        log.info("User info requested - userId: {}", user.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("provider", user.getProvider());
        response.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     * POST /users/logout
     */
    @Operation(
            summary = "로그아웃",
            description = "로그아웃하고 Redis에 저장된 Refresh Token을 삭제합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(value = "로그아웃되었습니다.")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        log.info("Logout requested - userId: {}", user.getUserId());

        authService.logout(user.getUserId());

        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    /**
     * 회원탈퇴
     * DELETE /users/me
     */
    @Operation(
            summary = "회원탈퇴",
            description = "회원탈퇴하고 DB와 Redis에서 모든 사용자 데이터를 삭제합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(value = "회원탈퇴가 완료되었습니다.")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal User user) {

        if (user == null) {
            log.warn("User is null in DELETE /user/me");
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        log.info("Account deletion requested - userId: {}, email: {}", user.getUserId(), user.getEmail());

        authService.deleteAccount(user.getUserId());

        return ResponseEntity.ok("회원탈퇴가 완료되었습니다.");
    }


    @Operation(
            summary = "판매자 상품 조회",
            description = "해당 유저가 판매중인 상품 목록을 가져옵니다."
    )
    // 판매자 상품 목록 조회
    @GetMapping("/{user}/products")
    @ResponseBody
    public ResponseEntity<Page<ProductListResponse>> getProductsWithUserId(
            @PathVariable User user,
            @PageableDefault(size = 20) Pageable pageable
    ){
        Page<ProductListResponse> response = productService.getProductsByUserId(user, pageable);
        log.info("getProductsWithUserId response: {}", response);
        return ResponseEntity.ok(response);
    }

}
