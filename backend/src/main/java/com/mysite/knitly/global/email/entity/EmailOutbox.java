package com.mysite.knitly.global.email.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_outbox")
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RabbitMQ 컨슈머가 사용할 DTO를 JSON 문자열로 저장합니다.
     * EmailNotificationDto의 직렬화된 형태입니다.
     */
    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public EmailOutbox(String payload) {
        this.payload = payload;
        this.status = EmailStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 상태 변경 메서드
    public void markAsPublished() {
        this.status = EmailStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount += 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailedToPublish() {
        this.status = EmailStatus.FAILED_TO_PUBLISH;
        this.updatedAt = LocalDateTime.now();
    }
}