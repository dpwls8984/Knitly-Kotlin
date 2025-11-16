package com.mysite.knitly.domain.community.post.dto

import com.mysite.knitly.domain.community.post.entity.PostCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PostCreateRequest(

    @field:NotNull(message = "카테고리는 필수입니다.")
    val category: PostCategory,

    @field:NotBlank(message = "제목은 필수입니다.")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다.")
    val content: String,

    val imageUrls: List<String>? = null
)
