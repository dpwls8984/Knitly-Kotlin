package com.mysite.knitly.global.email

import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.global.email.service.EmailService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class EmailNotificationConsumer(
    private val emailService: EmailService
) {
    companion object {
        private val log = LoggerFactory.getLogger(EmailNotificationConsumer::class.java)
    }

    @RabbitListener(queues = ["order.email.queue"])
    fun receiveOrderCompletionMessage(emailDto: EmailNotificationDto) {
        MDC.put("orderId", emailDto.orderId.toString())
        MDC.put("userId", emailDto.userId.toString())
        log.info("[EmailConsumer] [Receive] RabbitMQ 메시지 수신")

        try {
            emailService.sendOrderConfirmationEmail(emailDto)
            log.info("[EmailConsumer] [Receive] 이메일 발송 작업(EmailService) 성공")

        } catch (e: Exception) {
            log.error("[EmailConsumer] [Receive] 이메일 발송 작업(EmailService) 실패. RabbitMQ 재시도/DLQ로 전달.", e)
            throw RuntimeException("Email sending failed after processing.", e)
        } finally {
            MDC.clear()
        }
    }
}