package com.mysite.knitly.domain.product.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductListPageCache {
    private List<ProductListResponse> content;
    private long totalElements;
}
