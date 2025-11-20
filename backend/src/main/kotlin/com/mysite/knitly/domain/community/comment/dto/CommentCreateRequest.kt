package com.mysite.knitly.domain.community.comment.dto

data class CommentCreateRequest(
    val postId: Long,
    val parentId: Long?,
    val content: String
)
