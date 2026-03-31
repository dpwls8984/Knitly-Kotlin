package com.mysite.knitly.global.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class DiscordNotifier(
    private val restTemplate: RestTemplate,
    @Value("\${discord.webhook-url}") private val webhookUrl: String
) {
    companion object {
        private val log = LoggerFactory.getLogger(DiscordNotifier::class.java)
    }

    fun send(message: String) {
        try {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON

            val body = mapOf("content" to message)
            val request = HttpEntity(body, headers)

            restTemplate.postForEntity(webhookUrl, request, String::class.java)
            log.info("[Discord] 알림 전송 성공")
        } catch (e: Exception) {
            log.error("[Discord] 알림 전송 실패 - message={}", message, e)
        }
    }
}
