package com.mysite.knitly.global.email;

import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
import com.mysite.knitly.global.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final EmailService emailService;

    /**
     * RabbitMQConfig에서 설정한 'order.email.queue'를 리스닝
     */
    @RabbitListener(queues = "order.email.queue")
    public void receiveOrderCompletionMessage(EmailNotificationDto emailDto) {
        MDC.put("orderId", emailDto.orderId().toString());
        MDC.put("userId", emailDto.userId().toString());
        log.info("[EmailConsumer] [Receive] RabbitMQ 메시지 수신");

        try {
            // 실제 이메일 발송 로직 호출
            emailService.sendOrderConfirmationEmail(emailDto);
            log.info("[EmailConsumer] [Receive] 이메일 발송 작업(EmailService) 성공");

        } catch (Exception e) {
            log.error("[EmailConsumer] [Receive] 이메일 발송 작업(EmailService) 실패. RabbitMQ 재시도/DLQ로 전달.", e);
            // 예외를 다시 던져서 RabbitMQ가 재시도하거나 DLQ로 보내도록 함
            throw new RuntimeException("Email sending failed after processing.", e);
        } finally {
            MDC.clear();
        }
    }
}