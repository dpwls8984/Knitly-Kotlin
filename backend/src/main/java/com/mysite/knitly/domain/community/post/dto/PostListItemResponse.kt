package com.mysite.knitly.domain.community.post.dto

import com.mysite.knitly.domain.community.post.entity.PostCategory
import java.time.LocalDateTime

data class PostListItemResponse(
    val id: Long,
    val category: PostCategory,
    val title: String,
    val excerpt: String,
    val authorDisplay: String,
    val createdAt: LocalDateTime?,
    val commentCount: Long,
    val thumbnailUrl: String? = null
)
