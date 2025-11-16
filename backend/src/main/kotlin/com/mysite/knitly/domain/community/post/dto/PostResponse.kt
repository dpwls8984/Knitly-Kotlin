package com.mysite.knitly.domain.community.post.dto

import com.mysite.knitly.domain.community.post.entity.PostCategory
import java.time.LocalDateTime

data class PostResponse(
    val id: Long,
    val category: PostCategory,
    val title: String,
    val content: String,
    val imageUrls: List<String>,
    val authorId: Long,
    val authorDisplay: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val commentCount: Long,
    val mine: Boolean
)
