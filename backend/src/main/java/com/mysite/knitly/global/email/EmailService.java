package com.mysite.knitly.global.email;

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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final OrderRepository orderRepository;
    private final FileStorageService fileStorageService; // PDF 파일을 읽기 위해 주입

    @Transactional(readOnly = true) // 이메일 발송은 DB를 변경하지 않으므로 readOnly
    public void sendOrderConfirmationEmail(EmailNotificationDto emailDto) {
        // 1. DTO의 정보로 DB에서 전체 주문 정보를 다시 조회 (OrderItem, Product, Design까지 모두)
        Order order = orderRepository.findOrderWithDetailsById(emailDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + emailDto.orderId()));

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8"); // true: multipart
            mimeMessageHelper.setTo(emailDto.userEmail());
            mimeMessageHelper.setSubject("[Knitly] 주문하신 도안이 도착했습니다.");

            // 2. TODO: Thymeleaf 같은 템플릿 엔진을 사용하여 HTML 이메일 본문 생성
            String emailContent = String.format("<h1>%s님, 주문해주셔서 감사합니다.</h1><p>주문 번호: %d</p>",
                    order.getUser().getName(), order.getOrderId());
            mimeMessageHelper.setText(emailContent, true); // true: HTML

            // 3. 주문된 모든 상품의 PDF를 첨부
            for (OrderItem item : order.getOrderItems()) {
                String pdfUrl = item.getProduct().getDesign().getPdfUrl();
                try {
                    byte[] pdfBytes = fileStorageService.loadFileAsBytes(pdfUrl); // FileStorageService에 파일 읽기 기능 추가
                    mimeMessageHelper.addAttachment(item.getProduct().getTitle() + ".pdf", new ByteArrayResource(pdfBytes));
                } catch (IOException e) {
                    log.error("PDF 파일 첨부 실패: url={}", pdfUrl, e);
                    // 하나의 PDF 실패가 전체 이메일 발송을 막을지, 아니면 그냥 보낼지 정책 결정 필요
                }
            }

            // 4. 이메일 발송
            javaMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            // Consumer가 이 예외를 받아서 재시도/DLQ 처리
            throw new RuntimeException("MimeMessage 생성 또는 발송에 실패했습니다.", e);
        }
    }
}