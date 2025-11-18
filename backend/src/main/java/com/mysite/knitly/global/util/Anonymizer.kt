package com.mysite.knitly.global.util

import java.util.*
import kotlin.math.abs

// 같은 사용자(userId)면 언제나 같은 별칭을 얻도록 하는 유틸.
// 익명의 털실-1234 형식 (댓글의 순번 규칙은 서비스 로직에서 처리)
object Anonymizer {
    private const val PREFIX: String = "익명의 털실-"

    @JvmStatic
    fun yarn(userId: Long?): String {
        if (userId == null) return PREFIX + "0000"
        val hash = abs(userId.toString().lowercase(Locale.ROOT).hashCode())
        return PREFIX + String.format("%04d", hash % 10000)
    }
}