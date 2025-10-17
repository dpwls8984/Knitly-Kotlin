package com.mysite.knitly.domain.product.product.dto;

public record ProductModifyRequest(
        String description,
        String productCategory,
        String sizeInfo,
        Integer stockQuantity
){
}
