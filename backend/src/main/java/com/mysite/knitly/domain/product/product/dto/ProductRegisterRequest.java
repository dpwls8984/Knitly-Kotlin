package com.mysite.knitly.domain.product.product.dto;

public record ProductRegisterRequest(
        String title,
        String description,
        String productCategory,
        String sizeInfo,
        Double price,
        Integer stockQuantity
) {
}
