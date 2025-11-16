package com.mysite.knitly.domain.mypage.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class MyPostListItemResponse(
    val id: Long,
    val title: String,
    val excerpt: String,
    val thumbnailUrl: String?,
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd'T'HH:mm:ss",
        timezone = "Asia/Seoul"
    )
    val createdAt: LocalDateTime
)
