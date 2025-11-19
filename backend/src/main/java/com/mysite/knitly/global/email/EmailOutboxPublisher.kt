package com.mysite.knitly.global.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.global.email.entity.EmailOutbox
import com.mysite.knitly.global.email.entity.EmailStatus
import com.mysite.knitly.global.email.repository.EmailOutboxRepository
import com.mysite.knitly.global.lock.RedisLockService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EmailOutboxPublisher(
    private val emailOutboxRepository: EmailOutboxRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val redisLockService: RedisLockService,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val log = LoggerFactory.getLogger(EmailOutboxPublisher::class.java)
        private const val SCHEDULER_LOCK_KEY = "emailOutboxPublisherLock"
        private const val MAX_PUBLISH_RETRIES = 3
        private const val CLEANUP_DAYS = 30L
    }

    @Scheduled(fixedDelay = 60000)
    fun publishPendingEmails() {
        MDC.put("task", "EmailOutboxPublisher")
        log.info("[OutboxPublisher] [Run] 스케줄러 실행")

        if (!redisLockService.tryLock(SCHEDULER_LOCK_KEY)) {
            log.info("[OutboxPublisher] [Lock] 락 획득 실패. (다른 인스턴스 실행 중)")
            MDC.clear()
            return
        }

        log.info("[OutboxPublisher] [Lock] 락 획득 성공.")
        try {
            val jobs = emailOutboxRepository
                .findByStatusAndRetryCountLessThan(EmailStatus.PENDING, MAX_PUBLISH_RETRIES)

            if (jobs.isEmpty()) {
                log.info("[OutboxPublisher] [Run] PENDING 작업 0건 조회.")
            } else {
                log.info("[OutboxPublisher] [Run] PENDING 작업 {}건 조회.", jobs.size)
                jobs.forEach { processJob(it) }
            }
        } catch (e: Exception) {
            log.error("[OutboxPublisher] [Run] 스케줄러 실행 중 예외 발생", e)
        } finally {
            redisLockService.unlock(SCHEDULER_LOCK_KEY)
            log.info("[OutboxPublisher] [Lock] 락 해제.")
            MDC.clear()
        }
    }

    @Transactional
    fun processJob(job: EmailOutbox) {
        MDC.put("outboxId", job.id.toString())
        log.info("[OutboxPublisher] [Process] 작업 처리 시작")

        try {
            val emailDto: EmailNotificationDto = objectMapper.readValue(
                job.payload,
                object : com.fasterxml.jackson.core.type.TypeReference<EmailNotificationDto>() {}
            )
            MDC.put("orderId", emailDto.orderId.toString())
            log.debug("[OutboxPublisher] [Process] 페이로드 파싱 완료")

            rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.completed",
                objectMapper.writeValueAsString(emailDto)
            )
            log.debug("[OutboxPublisher] [Process] RabbitMQ 발행 요청 완료")

            job.markAsPublished()
            emailOutboxRepository.save(job)
            log.info("[OutboxPublisher] [Process] Outbox 상태 'PUBLISHED'로 변경 완료")

        }
//        catch (e: JsonProcessingException) {
//            log.error("[OutboxPublisher] [Process] JSON 파싱 실패. FAILED 처리. payload={}", job.payload, e)
//            job.markAsFailedToPublish()
//            emailOutboxRepository.save(job)
//        }
        catch (e: Exception) {
            log.warn("[OutboxPublisher] [Process] RabbitMQ 발행 실패 (재시도). retryCount={}", job.retryCount + 1, e)
            job.incrementRetryCount()
            if (job.retryCount >= MAX_PUBLISH_RETRIES) {
                job.markAsFailedToPublish()
                log.warn("[OutboxPublisher] [Process] 최대 재시도 횟수 초과. FAILED 처리.")
            }
            emailOutboxRepository.save(job)
        } finally {
            MDC.clear()
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanupOldOutboxEmails() {
        MDC.put("task", "EmailOutboxCleanup")
        log.info("[OutboxPublisher] [Cleanup] 오래된 Outbox 작업 정리 시작...")
        try {
            val cutoffDate = LocalDateTime.now().minusDays(CLEANUP_DAYS)
            val statusesToDelete = listOf(EmailStatus.PUBLISHED, EmailStatus.FAILED_TO_PUBLISH)

            emailOutboxRepository.deleteByStatusInAndCreatedAtBefore(statusesToDelete, cutoffDate)

            log.info("[OutboxPublisher] [Cleanup] {}일 이전의 PUBLISHED/FAILED 작업 정리 완료.", CLEANUP_DAYS)
        } catch (e: Exception) {
            log.error("[OutboxPublisher] [Cleanup] Outbox 작업 정리 중 예외 발생", e)
        } finally {
            MDC.clear()
        }
    }
}