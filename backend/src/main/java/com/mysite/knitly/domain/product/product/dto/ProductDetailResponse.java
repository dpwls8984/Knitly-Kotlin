package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;

import java.util.List;

public record ProductDetailResponse(
        Long productId,
        String title,
        String description,
        ProductCategory productCategory,
        String sizeInfo,
        Double price,
        //String createdAt,
        Integer stockQuantity,

        Integer likeCount,
        //Long designId
        Double avgReviewRating,
        List<String> productImageUrls
) {
    public static ProductDetailResponse from(Product product, List<String> imageUrls) {
        return new ProductDetailResponse(
                product.getProductId(),
                product.getTitle(),
                product.getDescription(),
                product.getProductCategory(),
                product.getSizeInfo(),
                product.getPrice(),
                //product.getCreatedAt().toString(),
                product.getStockQuantity(),
                product.getLikeCount(),
                //product.getDesign().getDesignId()
                product.getAvgReviewRating(),
                imageUrls
        );
    }
}
