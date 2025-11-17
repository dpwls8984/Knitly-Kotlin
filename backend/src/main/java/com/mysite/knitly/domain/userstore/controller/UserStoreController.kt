package com.mysite.knitly.domain.userstore.controller;


import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.service.UserService;
import com.mysite.knitly.domain.userstore.dto.StoreDescriptionRequest;
import com.mysite.knitly.domain.userstore.service.UserStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Store", description = "판매자 스토어 API")
@Slf4j
@RestController
@RequestMapping("/userstore")
@Controller
@RequiredArgsConstructor
public class UserStoreController {

    private final UserService userService;
    private final UserStoreService userStoreService;

    /**
     * 스토어 설명 조회
     * GET /userstore/{userId}/description
     */
    @Operation(
            summary = "스토어 설명 조회",
            description = "판매자 스토어의 설명을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "description": "안녕하세요! 제 스토어에 오신 것을 환영합니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "스토어를 찾을 수 없음")
    })
    @GetMapping("/{userId}/description")
    public ResponseEntity<Map<String, String>> getStoreDescription(
            @PathVariable Long userId) {

        log.info("Fetching store description for userId: {}", userId);

        try {
            String description = userStoreService.getStoreDetail(userId);

            Map<String, String> response = new HashMap<>();
            response.put("description", description);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Store not found for userId: {}", userId);
            return ResponseEntity.status(404)
                    .body(Map.of("error", "스토어를 찾을 수 없습니다."));
        }
    }

    /**
     * 스토어 설명 업데이트
     * PUT /userstore/{userId}/description
     */
    @Operation(
            summary = "스토어 설명 업데이트",
            description = "판매자 스토어의 설명을 업데이트합니다. 본인의 스토어만 수정 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업데이트 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "스토어 설명이 업데이트되었습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - 본인의 스토어만 수정 가능"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "스토어를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인 필요"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{userId}/description")
    public ResponseEntity<?> updateStoreDescription(
            @PathVariable Long userId,
            @RequestBody StoreDescriptionRequest request,
            @AuthenticationPrincipal User currentUser) {

        // 🔥 권한 검증: 본인만 수정 가능
        if (!userId.equals(currentUser.getUserId())) {
            log.warn("Unauthorized store description update attempt - userId: {}, requestUserId: {}",
                    currentUser.getUserId(), userId);
            return ResponseEntity.status(403)
                    .body(Map.of("error", "본인의 스토어만 수정할 수 있습니다."));
        }

        log.info("Updating store description for userId: {}", userId);

        try {
            userStoreService.updateStoreDetail(userId, request.getDescription());
            return ResponseEntity.ok(Map.of("message", "스토어 설명이 업데이트되었습니다."));
        } catch (IllegalArgumentException e) {
            log.error("Store not found for userId: {}", userId);
            return ResponseEntity.status(404)
                    .body(Map.of("error", "스토어를 찾을 수 없습니다."));
        }
    }
}