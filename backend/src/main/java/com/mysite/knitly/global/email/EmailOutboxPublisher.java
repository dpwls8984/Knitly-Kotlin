package com.mysite.knitly.global.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
import com.mysite.knitly.global.config.RabbitMQConfig;
import com.mysite.knitly.global.email.entity.EmailOutbox;
import com.mysite.knitly.global.email.entity.EmailStatus;
import com.mysite.knitly.global.email.repository.EmailOutboxRepository;
import com.mysite.knitly.global.lock.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOutboxPublisher {

    private final EmailOutboxRepository emailOutboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedisLockService redisLockService; // OrderFacade에서 쓰던 락 서비스
    private final ObjectMapper objectMapper;

    private static final String SCHEDULER_LOCK_KEY = "emailOutboxPublisherLock";
    private static final int MAX_PUBLISH_RETRIES = 3; // RabbitMQ 발행 재시도 횟수
    private static final int CLEANUP_DAYS = 30; // 30일 지난 데이터 삭제

    /**
     * 1분마다 PENDING 상태의 이메일 작업을 RabbitMQ에 발행합니다.
     */
    @Scheduled(fixedDelay = 60000) // 1분에 한 번 실행
    public void publishPendingEmails() {
        MDC.put("task", "EmailOutboxPublisher");
        log.info("[OutboxPublisher] [Run] 스케줄러 실행");

        // 1. 분산 락 획득 (OrderFacade와 동일한 방식)
        if (!redisLockService.tryLock(SCHEDULER_LOCK_KEY)) {
            log.info("[OutboxPublisher] [Lock] 락 획득 실패. (다른 인스턴스 실행 중)");
            MDC.clear();
            return;
        }

        log.info("[OutboxPublisher] [Lock] 락 획득 성공.");
        try {
            // 2. PENDING 상태의 작업 조회
            List<EmailOutbox> jobs = emailOutboxRepository
                    .findByStatusAndRetryCountLessThan(EmailStatus.PENDING, MAX_PUBLISH_RETRIES);

            if (jobs.isEmpty()) {
                log.info("[OutboxPublisher] [Run] PENDING 작업 0건 조회.");
            } else {
                log.info("[OutboxPublisher] [Run] PENDING 작업 {}건 조회.", jobs.size());
                for (EmailOutbox job : jobs) {
                    processJob(job);
                }
            }
        } catch (Exception e) {
            log.error("[OutboxPublisher] [Run] 스케줄러 실행 중 예외 발생", e);
        } finally {
            // 3. 락 해제
            redisLockService.unlock(SCHEDULER_LOCK_KEY);
            log.info("[OutboxPublisher] [Lock] 락 해제.");
            MDC.clear();
        }
    }

    /**
     * 개별 작업을 처리 (트랜잭션 분리)
     */
    @Transactional
    public void processJob(EmailOutbox job) {
        MDC.put("outboxId", job.getId().toString());
        log.info("[OutboxPublisher] [Process] 작업 처리 시작");

        try {
            // 1. JSON 페이로드를 DTO로 변환
            EmailNotificationDto emailDto = objectMapper.readValue(job.getPayload(), EmailNotificationDto.class);
            MDC.put("orderId", emailDto.orderId().toString());
            log.debug("[OutboxPublisher] [Process] 페이로드 파싱 완료");

            // 2. RabbitMQ로 발행 (기존 Config의 Exchange와 Routing Key 사용)
            rabbitTemplate.convertAndSend(
                    "order.exchange",
                    "order.completed",
                    emailDto
            );
            log.debug("[OutboxPublisher] [Process] RabbitMQ 발행 요청 완료");

            // 3. 발행 성공 시 상태 변경
            job.markAsPublished();
            emailOutboxRepository.save(job);
            log.info("[OutboxPublisher] [Process] Outbox 상태 'PUBLISHED'로 변경 완료");

        } catch (JsonProcessingException e) {
            // 페이로드 자체가 깨진 경우, 재시도해도 소용없음
            log.error("[OutboxPublisher] [Process] JSON 파싱 실패. FAILED 처리.", e);
            job.markAsFailedToPublish();
            emailOutboxRepository.save(job);
        } catch (Exception e) {
            // RabbitMQ 접속 장애 등 일시적 오류
            log.warn("[OutboxPublisher] [Process] RabbitMQ 발행 실패 (재시도). retryCount={}", job.getRetryCount() + 1, e);
            job.incrementRetryCount();
            if (job.getRetryCount() >= MAX_PUBLISH_RETRIES) {
                job.markAsFailedToPublish();
                log.warn("[OutboxPublisher] [Process] 최대 재시도 횟수 초과. FAILED 처리.");
            }
            emailOutboxRepository.save(job);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 매일 새벽 3시에 오래된 작업들을 DB에서 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldOutboxEmails() {
        MDC.put("task", "EmailOutboxCleanup");
        log.info("[OutboxPublisher] [Cleanup] 오래된 Outbox 작업 정리 시작...");
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(CLEANUP_DAYS);
            List<EmailStatus> statusesToDelete = List.of(EmailStatus.PUBLISHED, EmailStatus.FAILED_TO_PUBLISH);

            emailOutboxRepository.deleteByStatusInAndCreatedAtBefore(statusesToDelete, cutoffDate);

            log.info("[OutboxPublisher] [Cleanup] {}일 이전의 PUBLISHED/FAILED 작업 정리 완료.", CLEANUP_DAYS);
        } catch (Exception e) {
            log.error("[OutboxPublisher] [Cleanup] Outbox 작업 정리 중 예외 발생", e);
        } finally {
            MDC.clear();
        }
    }
}