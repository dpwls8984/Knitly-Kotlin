package com.mysite.knitly.domain.mypage.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class MyCommentListItem(
    val commentId: Long,
    val postId: Long,

    // 문자열(ISO)로 내려주고 KST 적용
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd'T'HH:mm:ss",
        timezone = "Asia/Seoul"
    )
    val createdAt: LocalDateTime,

    val preview: String
)
