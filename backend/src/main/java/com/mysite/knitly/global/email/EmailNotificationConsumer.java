package com.mysite.knitly.global.email;

import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "order.email.queue")
    public void receiveOrderCompletionMessage(EmailNotificationDto emailDto) {
        log.info("Received message for order: {}", emailDto.orderId());
        try {
            emailService.sendOrderConfirmationEmail(emailDto);
            log.info("Successfully sent email for order: {}", emailDto.orderId());
        } catch (Exception e) {
            log.error("Failed to send email for order: {}. Error: {}", emailDto.orderId(), e.getMessage());
            // 🚨 예외를 다시 던져서 RabbitMQ가 재시도하거나 DLQ로 보내도록 함
            throw new RuntimeException("Email sending failed after processing.", e);
        }
    }
}
