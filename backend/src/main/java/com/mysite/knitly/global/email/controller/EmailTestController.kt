package com.mysite.knitly.global.email.controller

import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.global.email.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 이메일 유실 테스트용 임시 컨트롤러 (dev 프로필 전용)
 * 테스트 완료 후 반드시 삭제할 것
 */
@Profile("dev")
@RestController
@RequestMapping("/test/email")
class EmailTestController(
    private val emailService: EmailService,
    @Qualifier("emailExecutor") private val emailExecutor: ThreadPoolTaskExecutor
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/send")
    fun triggerEmail(
        @RequestParam orderId: Long,
        @RequestParam userEmail: String
    ): ResponseEntity<String> {
        log.info("[EmailTest] 이메일 발송 트리거 - orderId={}, email={}", orderId, userEmail)

        val emailDto = EmailNotificationDto(
            orderId = orderId,
            userId = 1L,
            userEmail = userEmail
        )

        emailService.sendOrderConfirmationEmail(emailDto)

        // 큐 상태 모니터링
        log.info(
            "[EmailTest] [큐 상태] activeThreads={}, queueSize={}, poolSize={}, completedTasks={}",
            emailExecutor.activeCount,
            emailExecutor.threadPoolExecutor.queue.size,
            emailExecutor.poolSize,
            emailExecutor.threadPoolExecutor.completedTaskCount
        )

        return ResponseEntity.ok("이메일 발송 요청 완료 (비동기). 10초 내에 서버를 종료하세요.")
    }
}
