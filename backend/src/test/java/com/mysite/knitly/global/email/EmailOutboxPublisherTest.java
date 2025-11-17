//package com.mysite.knitly.global.email;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
//import com.mysite.knitly.global.email.entity.EmailOutbox;
//import com.mysite.knitly.global.email.entity.EmailStatus;
//import com.mysite.knitly.global.email.repository.EmailOutboxRepository;
//import com.mysite.knitly.global.lock.RedisLockService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.slf4j.MDC;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.test.context.ActiveProfiles;
//import lombok.extern.slf4j.Slf4j;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@Slf4j
//@SpringBootTest
//@ActiveProfiles("test") // H2 DB를 사용하기 위해 application-test.yml 로드
//class EmailOutboxPublisherTest {
//
//    @Autowired
//    private EmailOutboxPublisher emailOutboxPublisher;
//
//    @Autowired
//    private EmailOutboxRepository emailOutboxRepository;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    // 실제 RabbitMQ나 Redis에 연결하지 않고, 호출 여부만 검증
//    @MockBean
//    private RabbitTemplate rabbitTemplate;
//
//    @MockBean
//    private RedisLockService redisLockService;
//
//    @BeforeEach
//    void setUp() {
//        emailOutboxRepository.deleteAll();
//    }
//
//    @Test
//    @DisplayName("publishPendingEmails: PENDING 작업을 조회하여 RabbitMQ에 발행하고 PUBLISHED로 변경")
//    void testPublishPendingEmails_Success() throws Exception {
//        // PENDING 상태의 EmailOutbox 작업을 DB에 저장
//        MDC.put("testName", "testPublishPendingEmails_Success");
//        log.info("[Test] [Given] PENDING 상태의 Outbox 작업 생성");
//        EmailNotificationDto emailDto = new EmailNotificationDto(1L, 100L, "test@knitly.com");
//        String payload = objectMapper.writeValueAsString(emailDto);
//        EmailOutbox pendingJob = EmailOutbox.builder().payload(payload).build();
//        emailOutboxRepository.save(pendingJob);
//
//        // Redis 락 획득 성공 모킹
//        when(redisLockService.tryLock(any(String.class))).thenReturn(true);
//
//        // 스케줄러 메서드 직접 호출
//        log.info("[Test] [When] publishPendingEmails 메서드 호출");
//        emailOutboxPublisher.publishPendingEmails();
//
//        // RabbitMQ의 convertAndSend가 정확한 인자와 함께 1회 호출되었는지 검증
//        log.info("[Test] [Then] RabbitTemplate.convertAndSend 검증");
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq("order.exchange"),
//                eq("order.completed"),
//                any(EmailNotificationDto.class)
//        );
//
//        // DB의 Outbox 작업 상태가 'PUBLISHED'로 변경되었는지 검증
//        log.info("[Test] [Then] DB 상태 'PUBLISHED' 검증");
//        EmailOutbox finishedJob = emailOutboxRepository.findById(pendingJob.getId()).orElseThrow();
//        assertThat(finishedJob.getStatus()).isEqualTo(EmailStatus.PUBLISHED);
//
//        // 락이 정상적으로 해제되었는지 검증
//        log.info("[Test] [Then] RedisLockService.unlock 검증");
//        verify(redisLockService, times(1)).unlock(any(String.class));
//        MDC.clear();
//    }
//}