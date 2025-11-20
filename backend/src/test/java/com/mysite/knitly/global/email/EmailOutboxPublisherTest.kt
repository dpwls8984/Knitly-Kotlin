package com.mysite.knitly.global.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.global.email.entity.EmailOutbox
import com.mysite.knitly.global.email.entity.EmailStatus
import com.mysite.knitly.global.email.repository.EmailOutboxRepository
import com.mysite.knitly.global.lock.RedisLockService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class EmailOutboxPublisherTest {

    @Autowired
    private lateinit var emailOutboxPublisher: EmailOutboxPublisher

    @Autowired
    private lateinit var emailOutboxRepository: EmailOutboxRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var rabbitTemplate: RabbitTemplate

    @MockBean
    private lateinit var redisLockService: RedisLockService

    @BeforeEach
    fun setUp() {
        emailOutboxRepository.deleteAll()
    }

    @Test
    @DisplayName("publishPendingEmails: PENDING 작업을 조회하여 RabbitMQ에 발행하고 PUBLISHED로 변경")
    fun testPublishPendingEmails_Success() {
        // Given
        val emailDto = EmailNotificationDto(1L, 100L, "test@knitly.com")
        val payload = objectMapper.writeValueAsString(emailDto)
        val pendingJob = EmailOutbox.create(payload)
        emailOutboxRepository.save(pendingJob)

        given(redisLockService.tryLock(any())).willReturn(true)

        // When
        emailOutboxPublisher.publishPendingEmails()

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("order.exchange"),
            eq("order.completed"),
            any<String>() // JSON 문자열이므로 String 타입으로 매칭
        )

        val finishedJob = emailOutboxRepository.findById(pendingJob.id!!).orElseThrow()
        assertThat(finishedJob.status).isEqualTo(EmailStatus.PUBLISHED)

        verify(redisLockService, times(1)).unlock(any())
    }
}