package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ProductRegisterRequest(
        @Pattern(regexp = "^[a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\s~!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]+$", message = "상품 이름에는 한글, 영어, 숫자, 일부 특수문자만 사용할 수 있습니다.")
        String title,

        @Pattern(regexp = "^[a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\s~!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]+$", message = "상품 이름에는 한글, 영어, 숫자, 일부 특수문자만 사용할 수 있습니다.")
        String description,

        @NotNull(message = "상품 카테고리는 필수입니다.")
        ProductCategory productCategory,

        @Pattern(regexp = "^[a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\s~!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]+$", message = "사이즈 정보에는 한글, 영어, 숫자, 일부 특수문자만 사용할 수 있습니다.")
        String sizeInfo,

        @Min(value = 0, message = "가격은 반드시 0원 이상이어야 합니다.")
        Double price,

        @Size(max = 10, message = "상품 이미지는 최대 10개까지 등록할 수 있습니다.")
        List<MultipartFile> productImageUrls,

        Integer stockQuantity
) {
}
