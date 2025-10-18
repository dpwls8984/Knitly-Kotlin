package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ProductModifyRequest(
        @Pattern(regexp = "^[a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\s~!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]+$", message = "사이즈 정보에는 한글, 영어, 숫자, 일부 특수문자만 사용할 수 있습니다.")
        String description,

        @NotNull(message = "상품 카테고리는 필수입니다.")
        ProductCategory productCategory,

        @Pattern(regexp = "^[a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\s~!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]+$", message = "사이즈 정보에는 한글, 영어, 숫자, 일부 특수문자만 사용할 수 있습니다.")
        String sizeInfo,

        Integer stockQuantity
){
}
