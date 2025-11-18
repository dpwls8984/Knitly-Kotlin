package com.mysite.knitly.global.util

object FileNameUtils {

    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return "design"

        val sanitized = input.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")

        val withExtension = when {
            sanitized.lowercase().endsWith(".pdf") -> sanitized
            else -> "$sanitized.pdf"
        }

        return when {
            withExtension.length > 80 -> withExtension.substring(0, 80)
            else -> withExtension
        }
    }
}