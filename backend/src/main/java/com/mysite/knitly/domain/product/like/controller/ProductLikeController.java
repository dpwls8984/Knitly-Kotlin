package com.mysite.knitly.domain.product.like.controller;

import com.mysite.knitly.domain.product.like.service.ProductLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// TODO: [프론트엔드] 찜 등록,삭제 연타하더라도 한번만 적용되게 구현하기
@RequiredArgsConstructor
@RestController
@RequestMapping("/products/{productId}/like")
public class ProductLikeController {
    private final ProductLikeService productLikeService;

    @PostMapping
    public ResponseEntity<Void> addLike(@PathVariable Long productId) {
        // TODO: 현재는 userId를 임시로 씀. 추후 소셜로그인 기능 머지후에 수정예정
        UUID temporaryUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        productLikeService.addLike(temporaryUserId, productId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteLike(@PathVariable Long productId) {
        // TODO: 현재는 userId를 임시로 씀. 추후 소셜로그인 기능 머지후에 수정예정
        UUID temporaryUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        productLikeService.deleteLike(temporaryUserId, productId);

        return ResponseEntity.ok().build();
    }
}
