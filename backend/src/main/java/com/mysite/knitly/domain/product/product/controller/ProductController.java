package com.mysite.knitly.domain.product.product.controller;


import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductModifyResponse;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterResponse;
import com.mysite.knitly.domain.product.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my/products")
public class ProductController {

    private final ProductService productService;

    //TODO: userId 삭제

    @PostMapping("/{designId}/sale")
    public ResponseEntity<ProductRegisterResponse> registerProduct(
            @PathVariable("userId") Long userId,
            @PathVariable Long designId,
            @RequestBody @Valid ProductRegisterRequest request
    ) {
        ProductRegisterResponse response = productService.registerProduct(userId, designId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/modify")
    public ResponseEntity<ProductModifyResponse> modifyProduct(
            @PathVariable("userId") Long userId,
            @PathVariable Long productId,
            @RequestBody @Valid ProductModifyRequest request
    ) {
        ProductModifyResponse response = productService.modifyProduct(userId, productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("userId") Long userId,
            @PathVariable Long productId
    ) {
        productService.deleteProduct(userId, productId);
        return ResponseEntity.noContent().build();
    }
}
