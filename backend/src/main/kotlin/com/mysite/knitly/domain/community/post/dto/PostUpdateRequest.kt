package com.mysite.knitly.domain.community.post.dto

import com.mysite.knitly.domain.community.post.entity.PostCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PostUpdateRequest(

    @field:NotNull(message = "카테고리를 선택해 주세요.")
    val category: PostCategory,

    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다.")
    val content: String,

    @field:Size(max = 5, message = "이미지는 최대 5개까지 업로드할 수 있습니다.")
    val imageUrls: List<String>? = null
)
