package com.mysite.knitly.global.email.service

import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.global.util.FileStorageService
import jakarta.mail.MessagingException
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
    private val orderRepository: OrderRepository,
    private val fileStorageService: FileStorageService,
    private val paymentRepository: PaymentRepository,
    private val resourceLoader: ResourceLoader // ResourceLoader 주입
) {
    companion object {
        private val log = LoggerFactory.getLogger(EmailService::class.java)
        private const val TEMPLATE_PATH = "classpath:templates/email/order-confirm.html"
    }

    /**
     * 주문 확인 이메일을 발송하고 도안 파일을 첨부합니다.
     */
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

            // 1. 데이터 준비
            val payment = paymentRepository.findByOrder_OrderId(order.orderId!!)
            val paymentMethod = when (payment?.paymentMethod) {
                PaymentMethod.CARD -> "카드 결제"
                PaymentMethod.VIRTUAL_ACCOUNT -> "가상계좌"
                PaymentMethod.EASY_PAY -> "간편 결제"
                else -> "결제수단 정보 없음"
            }
            val formattedDate = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm:ss")
                .withZone(ZoneId.of("Asia/Seoul"))
                .format(order.createdAt)

            // 2. 주문 상품 HTML 조각 생성
            val orderItemsHtml = buildOrderItemsHtml(order)

            // 3. 템플릿 로드 및 렌더링
            val emailContent = renderHtml(
                order,
                paymentMethod,
                formattedDate,
                orderItemsHtml
            )

            mimeMessageHelper.setText(emailContent, true)
            log.debug("[EmailService] [Send] 이메일 본문(HTML 템플릿) 생성 완료")

            // 4. PDF 첨부 및 발송
            attachPdfsAndSend(order, mimeMessageHelper)

            log.info("[EmailService] [Send] 이메일 발송 API 호출 성공 - to={}", emailDto.userEmail)
        } catch (e: MessagingException) {
            log.error("[EmailService] [Send] MimeMessage 생성 또는 Gmail 발송 실패. 작업 롤백/재시도.", e)
            throw RuntimeException("MimeMessage 생성 또는 발송에 실패했습니다.", e)
        }
    }

    /**
     * OrderItem 목록을 HTML 테이블 row(<tr>) 문자열로 변환합니다.
     */
    private fun buildOrderItemsHtml(order: Order): String {
        return order.orderItems.map { item ->
            // order-confirm.html의 {{orderItems}} 플레이스홀더에 들어갈 HTML 조각
            """
            <tr style="border-bottom:1px solid #eee;">
                <td style="padding:8px 15px;">${item.product!!.title}</td>
                <td style="padding:8px 15px;text-align:right;">₩%,.0f</td>
                <td style="padding:8px 15px;text-align:center;">${item.quantity}개</td>
            </tr>
            """.trimIndent().format(
                Locale.KOREA, // Locale 설정 추가 (숫자 포맷팅의 안정성 확보)
                item.orderPrice // ₩%,.0f 에 대응
            )
        }.joinToString("\n")
    }

    /**
     * HTML 템플릿을 로드하고 데이터를 주입하여 완성된 HTML을 반환합니다.
     */
    private fun renderHtml(
        order: Order,
        paymentMethod: String,
        formattedDate: String,
        orderItemsHtml: String
    ): String {
        val templateContent = loadHtmlTemplate()

        // 코틀린스럽게 replace 함수를 연속적으로 사용하여 플레이스홀더를 대체
        return templateContent
            .replace("{{username}}", order.user!!.name)
            .replace("{{orderId}}", order.orderId.toString())
            .replace("{{orderDate}}", formattedDate)
            .replace("{{paymentMethod}}", paymentMethod)
            .replace("{{orderItems}}", orderItemsHtml)
            .replace("{{totalPrice}}", String.format(Locale.KOREA, "%,.0f", order.totalPrice))
    }

    /**
     * Resources 폴더에서 HTML 템플릿 파일을 로드합니다.
     */
    private fun loadHtmlTemplate(): String {
        val resource = resourceLoader.getResource(TEMPLATE_PATH)
        return try {
            InputStreamReader(resource.inputStream, StandardCharsets.UTF_8).use { reader ->
                FileCopyUtils.copyToString(reader)
            }
        } catch (e: IOException) {
            log.error("[EmailService] [Template] HTML 템플릿 로드 실패: {}", TEMPLATE_PATH, e)
            throw RuntimeException("이메일 템플릿 로드 실패: $TEMPLATE_PATH", e)
        }
    }

    /**
     * PDF 파일을 첨부하고 이메일을 발송합니다.
     */
    private fun attachPdfsAndSend(order: Order, mimeMessageHelper: MimeMessageHelper) {
        order.orderItems.forEach { item ->
            val pdfUrl = item.product!!.design.pdfUrl
            log.debug("[EmailService] [Send] PDF 첨부파일 로드 시도 - url={}", pdfUrl)
            try {
                val pdfBytes = fileStorageService.loadFileAsBytes(pdfUrl)
                val filename = item.product!!.title + ".pdf"
                mimeMessageHelper.addAttachment(filename, ByteArrayResource(pdfBytes))
                log.debug("[EmailService] [Send] 파일 첨부 완료: {}", filename)
            } catch (e: IOException) {
                log.error("[EmailService] [Send] PDF 파일 첨부 실패. 작업 롤백/재시도. url={}", pdfUrl, e)
                throw RuntimeException("PDF 파일 로드 실패: $pdfUrl", e)
            }
        }

        javaMailSender.send(mimeMessageHelper.mimeMessage)
    }
}