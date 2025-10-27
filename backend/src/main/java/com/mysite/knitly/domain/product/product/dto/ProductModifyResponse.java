package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;

import java.util.List;

public record ProductModifyResponse (
        Long productId,
        String title,
        String description,
        ProductCategory productCategory,
        String sizeInfo,
        Integer stockQuantity,
        List<String> productImageUrls

){
    public static ProductModifyResponse from(Product product, List<String> imageUrls) {
        return new ProductModifyResponse(
                product.getProductId(),
                product.getTitle(),
                product.getDescription(),
                product.getProductCategory(),
                product.getSizeInfo(),
                product.getStockQuantity(),
                imageUrls
        );
    }
}
