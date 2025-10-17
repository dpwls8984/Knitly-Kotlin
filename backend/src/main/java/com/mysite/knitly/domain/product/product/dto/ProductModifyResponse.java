package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.Product;

public record ProductModifyResponse (
        Long productId,
        String title,
        String description,
        String productCategory,
        String sizeInfo,
        Integer stockQuantity
){
    public static ProductModifyResponse from(Product product) {
        return new ProductModifyResponse(
                product.getProductId(),
                product.getTitle(),
                product.getDescription(),
                product.getProductCategory(),
                product.getSizeInfo(),
                product.getStockQuantity());
    }
}
