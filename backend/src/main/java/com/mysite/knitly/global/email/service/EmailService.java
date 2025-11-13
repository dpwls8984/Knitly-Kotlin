package com.mysite.knitly.global.email.service;

import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.global.util.FileStorageService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final OrderRepository orderRepository;
    private final FileStorageService fileStorageService;

    public void sendOrderConfirmationEmail(EmailNotificationDto emailDto) {
        log.info("[EmailService] [Send] 이메일 발송 처리 시작 - to={}", emailDto.userEmail());

        Order order = orderRepository.findOrderWithDetailsById(emailDto.orderId())
                .orElseThrow(() -> {
                    // 이 경우는 재시도해도 소용없으므로, 에러 로그만 남기고 예외를 던지지 않을 수 있음 (정책 결정)
                    // 여기서는 재시도를 위해 예외를 던짐.
                    log.error("[EmailService] [Send] DB에서 Order 엔티티 조회 실패. orderId={}", emailDto.orderId());
                    return new IllegalArgumentException("Order not found: " + emailDto.orderId());
                });
        log.debug("[EmailService] [Send] DB에서 Order 엔티티 조회 완료");

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            mimeMessageHelper.setTo(emailDto.userEmail());
            mimeMessageHelper.setSubject("[Knitly] 주문하신 도안이 도착했습니다.");

            String emailContent = String.format("<h1>%s님, 주문해주셔서 감사합니다.</h1><p>주문 번호: %d</p>",
                    order.getUser().getName(), order.getOrderId());
            mimeMessageHelper.setText(emailContent, true); // true: HTML
            log.debug("[EmailService] [Send] 이메일 본문 생성 완료");

            // 3. 주문된 모든 상품의 PDF를 첨부
            for (OrderItem item : order.getOrderItems()) {
                String pdfUrl = item.getProduct().getDesign().getPdfUrl();
                log.debug("[EmailService] [Send] PDF 첨부파일 로드 시도 - url={}", pdfUrl);
                try {
                    byte[] pdfBytes = fileStorageService.loadFileAsBytes(pdfUrl);
                    mimeMessageHelper.addAttachment(item.getProduct().getTitle() + ".pdf", new ByteArrayResource(pdfBytes));
                } catch (IOException e) {
                    // PDF 첨부 실패 시, 예외를 던져 RabbitMQ가 재시도하도록 함
                    log.error("[EmailService] [Send] PDF 파일 첨부 실패. 작업 롤백/재시도. url={}", pdfUrl, e);
                    throw new RuntimeException("PDF 파일 로드 실패: " + pdfUrl, e);
                }
            }
            log.debug("[EmailService] [Send] 모든 PDF 첨부 완료");

            // 4. 이메일 발송
            javaMailSender.send(mimeMessage);
            log.info("[EmailService] [Send] 이메일 발송 API 호출 성공 - to={}", emailDto.userEmail());

        } catch (MessagingException e) {
            log.error("[EmailService] [Send] MimeMessage 생성 또는 Gmail 발송 실패. 작업 롤백/재시도.", e);
            // Consumer가 이 예외를 받아서 재시도/DLQ 처리
            throw new RuntimeException("MimeMessage 생성 또는 발송에 실패했습니다.", e);
        }
    }
}