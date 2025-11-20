package com.mysite.knitly.domain.community.comment.dto

import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val content: String,
    val authorId: Long,
    val authorDisplay: String,
    val createdAt: LocalDateTime?,
    val mine: Boolean
)
