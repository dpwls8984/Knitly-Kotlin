package com.mysite.knitly.domain.home.dto

import java.time.LocalDateTime

data class LatestPostItem(
    val postId: Long,
    val title: String,
    val category: String,
    val thumbnailUrl: String?,
    val createdAt: LocalDateTime
)