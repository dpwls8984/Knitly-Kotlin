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

    @PostMapping("/{designId}/sale")
    public ResponseEntity<ProductRegisterResponse> registerProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long designId,
            @RequestBody @Valid ProductRegisterRequest request
    ) {
        ProductRegisterResponse response = productService.registerProduct(user, designId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/modify")
    public ResponseEntity<ProductModifyResponse> modifyProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId,
            @RequestBody @Valid ProductModifyRequest request
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
}
