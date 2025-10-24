package com.mysite.knitly.domain.product.product.controller;


import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductModifyResponse;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterResponse;
import com.mysite.knitly.domain.product.product.service.ProductService;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my/products")
public class ProductController {

    private final ProductService productService;

    // 판매 등록
    @PostMapping("/{designId}/sale")
    public ResponseEntity<ProductRegisterResponse> registerProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long designId,
            @ModelAttribute @Valid ProductRegisterRequest request
    ) {
        ProductRegisterResponse response = productService.registerProduct(user, designId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/modify")
    public ResponseEntity<ProductModifyResponse> modifyProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId,
            @ModelAttribute @Valid ProductModifyRequest request
    ) {
        ProductModifyResponse response = productService.modifyProduct(user, productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId
    ) {
        productService.deleteProduct(user, productId);
        return ResponseEntity.noContent().build();
    }

    //재판매
    @PostMapping("/{productId}/relist")
    public ResponseEntity<Void> relistProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId
    ) {
        productService.relistProduct(user, productId);
        return ResponseEntity.ok().build();
    }
}
