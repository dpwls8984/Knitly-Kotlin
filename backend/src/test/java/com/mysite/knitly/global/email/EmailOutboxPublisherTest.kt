//package com.mysite.knitly.global.email
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.mysite.knitly.domain.order.dto.EmailNotificationDto
//import com.mysite.knitly.global.email.entity.EmailOutbox
//import com.mysite.knitly.global.email.entity.EmailStatus
//import com.mysite.knitly.global.email.repository.EmailOutboxRepository
//import com.mysite.knitly.global.lock.RedisLockService
//import lombok.extern.slf4j.Slf4j
//import org.assertj.core.api.Assertions
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//import org.mockito.ArgumentMatchers
//import org.mockito.Mockito
//import org.slf4j.MDC
//import org.springframework.amqp.rabbit.core.RabbitTemplate
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.mock.mockito.MockBean
//import org.springframework.test.context.ActiveProfiles
//
//@Slf4j
//@SpringBootTest
//@ActiveProfiles("test")
//internal class EmailOutboxPublisherTest {
//    @Autowired
//    private val emailOutboxPublisher: EmailOutboxPublisher? = null
//
//    @Autowired
//    private val emailOutboxRepository: EmailOutboxRepository? = null
//
//    @Autowired
//    private val objectMapper: ObjectMapper? = null
//
//    // 실제 RabbitMQ나 Redis에 연결하지 않고, 호출 여부만 검증
//    @MockBean
//    private val rabbitTemplate: RabbitTemplate? = null
//
//    @MockBean
//    private val redisLockService: RedisLockService? = null
//
//    @BeforeEach
//    fun setUp() {
//        emailOutboxRepository!!.deleteAll()
//    }
//
//    @Test
//    @DisplayName("publishPendingEmails: PENDING 작업을 조회하여 RabbitMQ에 발행하고 PUBLISHED로 변경")
//    @Throws(
//        Exception::class
//    )
//    fun testPublishPendingEmails_Success() {
//        // PENDING 상태의 EmailOutbox 작업을 DB에 저장
//        MDC.put("testName", "testPublishPendingEmails_Success")
//        EmailOutboxPublisherTest.log.info("[Test] [Given] PENDING 상태의 Outbox 작업 생성")
//        val emailDto = EmailNotificationDto(1L, 100L, "test@knitly.com")
//        val payload = objectMapper!!.writeValueAsString(emailDto)
//        val pendingJob: EmailOutbox = EmailOutbox.builder().payload(payload).build()
//        emailOutboxRepository!!.save(pendingJob)
//
//        // Redis 락 획득 성공 모킹
//        Mockito.`when`(redisLockService!!.tryLock(ArgumentMatchers.any(String::class.java))).thenReturn(true)
//
//        // 스케줄러 메서드 직접 호출
//        EmailOutboxPublisherTest.log.info("[Test] [When] publishPendingEmails 메서드 호출")
//        emailOutboxPublisher!!.publishPendingEmails()
//
//        // RabbitMQ의 convertAndSend가 정확한 인자와 함께 1회 호출되었는지 검증
//        EmailOutboxPublisherTest.log.info("[Test] [Then] RabbitTemplate.convertAndSend 검증")
//        Mockito.verify(rabbitTemplate, Mockito.times(1)).convertAndSend(
//            ArgumentMatchers.eq("order.exchange"),
//            ArgumentMatchers.eq("order.completed"),
//            ArgumentMatchers.any(EmailNotificationDto::class.java)
//        )
//
//        // DB의 Outbox 작업 상태가 'PUBLISHED'로 변경되었는지 검증
//        EmailOutboxPublisherTest.log.info("[Test] [Then] DB 상태 'PUBLISHED' 검증")
//        val finishedJob = emailOutboxRepository.findById(pendingJob.id!!).orElseThrow()
//        Assertions.assertThat(finishedJob.status).isEqualTo(EmailStatus.PUBLISHED)
//
//        // 락이 정상적으로 해제되었는지 검증
//        EmailOutboxPublisherTest.log.info("[Test] [Then] RedisLockService.unlock 검증")
//        Mockito.verify(redisLockService, Mockito.times(1)).unlock(
//            ArgumentMatchers.any(
//                String::class.java
//            )
//        )
//        MDC.clear()
//    }
//}