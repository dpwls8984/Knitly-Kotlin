package com.mysite.knitly.domain.mypage.dto

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
) {
    companion object {
        @JvmStatic
        fun <T> of(p: Page<T>): PageResponse<T> {
            return PageResponse(
                content = p.content,
                page = p.number,
                size = p.size,
                totalElements = p.totalElements,
                totalPages = p.totalPages,
                first = p.isFirst,
                last = p.isLast
            )
        }
    }
}
