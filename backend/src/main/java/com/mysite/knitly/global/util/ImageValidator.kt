package com.mysite.knitly.global.util

import java.util.regex.Pattern

object ImageValidator {

    private val ALLOWED: Pattern =
        Pattern.compile("(?i).+\\.(png|jpg|jpeg)(\\?.*)?$")

    @JvmStatic
    fun isAllowedImageUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return true  // 이미지 미첨부는 통과
        return ALLOWED.matcher(url).matches()
    }
}