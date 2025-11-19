package com.mysite.knitly.global.email.service

import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.global.util.FileStorageService
import jakarta.mail.MessagingException
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
    private val orderRepository: OrderRepository,
    private val fileStorageService: FileStorageService,
    private val paymentRepository: PaymentRepository
) {
    companion object {
        private val log = LoggerFactory.getLogger(EmailService::class.java)
    }

    fun sendOrderConfirmationEmail(emailDto: EmailNotificationDto) {
        log.info("[EmailService] [Send] 이메일 발송 처리 시작 - to={}", emailDto.userEmail)

        val order = orderRepository.findById(emailDto.orderId?.toLong() ?: 0L)
            .orElseThrow {
                log.error("[EmailService] [Send] DB에서 Order 엔티티 조회 실패. orderId={}", emailDto.orderId)
                IllegalArgumentException("Order not found: " + emailDto.orderId)
            }
        log.debug("[EmailService] [Send] DB에서 Order 엔티티 조회 완료")

        val mimeMessage = javaMailSender.createMimeMessage()

        try {
            val mimeMessageHelper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            mimeMessageHelper.setTo(emailDto.userEmail)
            mimeMessageHelper.setSubject("[Knitly] 주문하신 도안이 도착했습니다.")

            val payment = paymentRepository.findByOrder_OrderId(order.orderId!!)

            val paymentMethod = when (payment?.paymentMethod) {
                PaymentMethod.CARD -> "카드 결제"
                PaymentMethod.VIRTUAL_ACCOUNT -> "가상계좌"
                PaymentMethod.EASY_PAY -> " 간편 결제"
                PaymentMethod.FREE -> "무료 결제"
                null -> "결제수단 정보 없음"
            }

            val orderItemsHtml = order.orderItems.map { item ->
                """
                <tr style="border-bottom:1px solid #eee;">
                    <td style="padding:10px 15px;">%s</td>
                    <td style="padding:10px 15px;text-align:right;">₩%,.0f</td>
                    <td style="padding:10px 15px;text-align:center;">%d개</td>
                </tr>
                """.trimIndent().format(
                    item.product!!.title,
                    item.orderPrice,
                    item.quantity
                )
            }.joinToString("\n")

            val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm:ss")
                .withZone(ZoneId.of("Asia/Seoul"))
            val formattedDate = formatter.format(order.createdAt)

            val emailContent = """
            <div style="font-family:'Apple SD Gothic Neo','Noto Sans KR',sans-serif;
                        width:90%%;max-width:640px;margin:0 auto;background:#fafafa;
                        border-radius:14px;border:1px solid #e0e0e0;padding:40px 30px;">
                
                <h2 style="font-size:22px;font-weight:700;color:#333;margin-bottom:10px;">
                    %s님, 주문해주셔서 감사합니다 💐
                </h2>
                <p style="font-size:15px;color:#555;line-height:1.6;">
                    Knitly에서의 주문이 정상적으로 완료되었습니다.<br>
                    주문하신 도안은 첨부된 파일로 함께 발송됩니다.
                </p>
            
                <div style="background:#fff;border:1px solid #eee;border-radius:10px;padding:20px;margin-top:20px;">
                    <table style="width:100%%;font-size:15px;border-collapse:collapse;">
                        <tr><td style="color:#888;">주문 번호</td><td style="text-align:right;font-weight:bold;">#%d</td></tr>
                        <tr><td style="color:#888;">주문 시각</td><td style="text-align:right;">%s</td></tr>
                        <tr><td style="color:#888;">결제 수단</td><td style="text-align:right;">%s</td></tr>
                        <tr><td style="color:#888;">주문자</td><td style="text-align:right;">%s</td></tr>
                    </table>
                </div>
            
                <div style="margin-top:30px;background:#fff;border:1px solid #eee;border-radius:10px;">
                    <h3 style="padding:15px 20px;border-bottom:1px solid #eee;font-size:16px;color:#333;margin:0;">
                        🧶 주문 내역
                    </h3>
                    <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                        <thead>
                            <tr style="background:#f9f9f9;">
                                <th style="text-align:left;padding:10px 15px;">상품명</th>
                                <th style="text-align:right;padding:10px 15px;">가격</th>
                                <th style="text-align:center;padding:10px 15px;">수량</th>
                            </tr>
                        </thead>
                        <tbody>%s</tbody>
                    </table>
                    <div style="text-align:right;padding:20px;font-weight:bold;color:#333;border-top:1px solid #eee;">
                        총 결제 금액: ₩%,.0f
                    </div>
                </div>
            
                <div style="text-align:center;margin-top:40px;">
                    <a href="http://localhost:3000" target="_blank"
                       style="display:inline-block;background:#333;color:#fff;
                              text-decoration:none;padding:12px 24px;border-radius:6px;
                              font-size:15px;font-weight:500;">
                        Knitly 홈페이지로 가기
                    </a>
                </div>
            
                <p style="margin-top:40px;font-size:12px;color:#aaa;text-align:center;">
                    © 2025 Knitly. All rights reserved.
                </p>
            </div>
            """.trimIndent().format(
                order.user!!.name,
                order.orderId,
                formattedDate,
                paymentMethod,
                orderItemsHtml,
                order.totalPrice
            )

            mimeMessageHelper.setText(emailContent, true)
            log.debug("[EmailService] [Send] 이메일 본문(Text Block) 생성 완료")

            for (item in order.orderItems) {
                val pdfUrl = item.product!!.design.pdfUrl
                log.debug("[EmailService] [Send] PDF 첨부파일 로드 시도 - url={}", pdfUrl)
                try {
                    val pdfBytes = fileStorageService.loadFileAsBytes(pdfUrl)
                    mimeMessageHelper.addAttachment(item.product!!.title + ".pdf", ByteArrayResource(pdfBytes))
                } catch (e: IOException) {
                    log.error("[EmailService] [Send] PDF 파일 첨부 실패. 작업 롤백/재시도. url={}", pdfUrl, e)
                    throw RuntimeException("PDF 파일 로드 실패: $pdfUrl", e)
                }
            }
            log.debug("[EmailService] [Send] 모든 PDF 첨부 완료")

            javaMailSender.send(mimeMessage)
            log.info("[EmailService] [Send] 이메일 발송 API 호출 성공 - to={}", emailDto.userEmail)
        } catch (e: MessagingException) {
            log.error("[EmailService] [Send] MimeMessage 생성 또는 Gmail 발송 실패. 작업 롤백/재시도.", e)
            throw RuntimeException("MimeMessage 생성 또는 발송에 실패했습니다.", e)
        }
    }
}