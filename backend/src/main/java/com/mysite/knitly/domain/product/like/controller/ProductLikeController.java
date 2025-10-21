package com.mysite.knitly.domain.product.like.controller;

import com.mysite.knitly.domain.product.like.service.ProductLikeService;
import com.mysite.knitly.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// TODO: [프론트엔드] 찜 등록,삭제 연타하더라도 한번만 적용되게 구현하기
@RequiredArgsConstructor
@RestController
@RequestMapping("/products/{productId}/like")
public class ProductLikeController {
    private final ProductLikeService productLikeService;

    @PostMapping
    public ResponseEntity<Void> addLike(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId) {

        Long currentUserId = user.getUserId();
        productLikeService.addLike(currentUserId, productId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteLike(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId) {

        Long currentUserId = user.getUserId();
        productLikeService.deleteLike(currentUserId, productId);

        productLikeService.deleteLike(currentUserId, productId);

        return ResponseEntity.ok().build();
    }
}
