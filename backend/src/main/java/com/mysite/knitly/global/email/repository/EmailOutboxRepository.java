package com.mysite.knitly.global.email.repository;

import com.mysite.knitly.global.email.entity.EmailOutbox;
import com.mysite.knitly.global.email.entity.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    // RabbitMQ에 발행될 작업 조회
    // PENDING 상태이면서 재시도 횟수가 maxRetries (최대 재시도 횟수) 미만인 이메일 아웃박스 엔티티들을 조회
    List<EmailOutbox> findByStatusAndRetryCountLessThan(EmailStatus status, int maxRetries);

    // 오래된 이메일 아웃박스 엔티티 삭제, EmailOutboxPublisher의 cleanup 스케줄러에서 사용할 메서드
    void deleteByStatusInAndCreatedAtBefore(List<EmailStatus> statuses, LocalDateTime cutoffDate);
}
