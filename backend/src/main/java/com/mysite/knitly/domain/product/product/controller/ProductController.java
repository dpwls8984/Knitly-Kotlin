package com.mysite.knitly.domain.product.product.controller;


import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductModifyResponse;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterResponse;
import com.mysite.knitly.domain.product.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/{designId}/sale")
    public ResponseEntity<ProductRegisterResponse> registerProduct(
            //userId 이렇게 구현하면 보안 취약점 생길수있음. 나중에 고치기
            @PathVariable("userId") UUID userId,
            @PathVariable Long designId,
            @RequestBody ProductRegisterRequest request
    ) {
        ProductRegisterResponse response = productService.registerProduct(userId, designId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/modify")
    public ResponseEntity<ProductModifyResponse> modifyProduct(
            @PathVariable("userId") UUID userId,
            @PathVariable Long productId,
            @RequestBody ProductModifyRequest request
    ) {
        ProductModifyResponse response = productService.modifyProduct(userId, productId, request);
        return ResponseEntity.ok(response);
    }
}
