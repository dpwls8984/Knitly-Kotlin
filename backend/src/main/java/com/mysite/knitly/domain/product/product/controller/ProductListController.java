package com.mysite.knitly.domain.product.product.controller;

import com.mysite.knitly.domain.product.product.dto.ProductDetailResponse;
import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.entity.ProductFilterType;
import com.mysite.knitly.domain.product.product.entity.ProductSortType;
import com.mysite.knitly.domain.product.product.service.ProductService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductListController {
    private final ProductService productService;

    // 상품 목록 조회
    @GetMapping
    public ResponseEntity<Page<ProductListResponse>> getProducts(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false, defaultValue = "ALL") ProductFilterType filter,
            @RequestParam(required = false, defaultValue = "LATEST") ProductSortType sort,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductListResponse> response = productService.getProducts(category, filter, sort, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @RequestParam Long productId
    ) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(response);
    }
}
