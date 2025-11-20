package com.mysite.knitly.domain.product.product.dto

import com.mysite.knitly.domain.product.product.entity.ProductCategory
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class ProductModifyRequest(

    @field:Pattern(regexp = "^[a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\s~!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]+$", message = "사이즈 정보에는 한글, 영어, 숫자, 일부 특수문자만 사용할 수 있습니다.")
    val description: String?,

    val productCategory: ProductCategory?,

    @field:Pattern(regexp = "^[a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\s~!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?]+$", message = "사이즈 정보에는 한글, 영어, 숫자, 일부 특수문자만 사용할 수 있습니다.")
    val sizeInfo: String?,

    @field:Size(max = 10, message = "상품 이미지는 최대 10개까지 등록할 수 있습니다.")
    val productImageUrls: List<MultipartFile>?,

    val existingImageUrls: List<String>?,

    val stockQuantity: Int?
)