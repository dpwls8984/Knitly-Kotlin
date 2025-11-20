package com.mysite.knitly.global.email.service

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.util.FileStorageService
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.javamail.JavaMailSender
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class EmailServiceTest {

    @Mock private lateinit var javaMailSender: JavaMailSender
    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var fileStorageService: FileStorageService
    @Mock private lateinit var paymentRepository: PaymentRepository
    @Mock private lateinit var resourceLoader: ResourceLoader

    @Mock private lateinit var mockResource: Resource
    @Mock private lateinit var mockMimeMessage: MimeMessage

    @InjectMocks
    private lateinit var emailService: EmailService

    private lateinit var order: Order
    private lateinit var user: User
    private lateinit var emailDto: EmailNotificationDto

    @BeforeEach
    fun setUp() {
        user = User(userId = 1L, name = "테스터", email = "test@knitly.com", socialId = "123", provider = com.mysite.knitly.domain.user.entity.Provider.GOOGLE)

        order = Order(user = user, tossOrderId = "toss_123")
        setPrivateField(order, "orderId", 100L)
        setPrivateField(order, "createdAt", LocalDateTime.now())

        val design = Design(user = user, pdfUrl = "/resources/static/design/pattern.pdf", designName = "도안", gridData = "{}", designState = com.mysite.knitly.domain.design.entity.DesignState.ON_SALE)

        val product = Product(productId = 1L, title = "예쁜 니트", price = 10000.0, user = user, design = design, description = "", productCategory = com.mysite.knitly.domain.product.product.entity.ProductCategory.TOP, sizeInfo = "")

        val orderItem = OrderItem(product = product, orderPrice = 10000.0, quantity = 1)
        order.addOrderItem(orderItem)

        emailDto = EmailNotificationDto(orderId = 100L, userId = 1L, userEmail = "test@knitly.com")
    }

    @Test
    @DisplayName("이메일 발송 성공: HTML 템플릿 렌더링 및 PDF 첨부 파일 로드 검증")
    fun sendOrderConfirmationEmail_Success() {

        given(orderRepository.findById(100L)).willReturn(Optional.of(order))

        val payment = Payment(order = order, buyer = user, totalAmount = 10000, paymentMethod = PaymentMethod.CARD, paymentStatus = PaymentStatus.DONE, tossOrderId = "toss_123")
        given(paymentRepository.findByOrder_OrderId(100L)).willReturn(payment)
        given(javaMailSender.createMimeMessage()).willReturn(mockMimeMessage)

        val dummyHtml = "<html><body>User: {{username}}, Items: {{orderItems}}</body></html>"
        given(resourceLoader.getResource(any())).willReturn(mockResource)
        given(mockResource.inputStream).willReturn(ByteArrayInputStream(dummyHtml.toByteArray()))

        val pdfUrl = "/resources/static/design/pattern.pdf"
        given(fileStorageService.loadFileAsBytes(pdfUrl)).willReturn(byteArrayOf(1, 2, 3, 4, 5))
        emailService.sendOrderConfirmationEmail(emailDto)

        verify(orderRepository).findById(100L)
        verify(resourceLoader).getResource(any())
        verify(fileStorageService).loadFileAsBytes(pdfUrl)
        verify(javaMailSender).send(mockMimeMessage)
    }

    @Test
    @DisplayName("PDF 파일 로드 중 IOException 발생 시 RuntimeException으로 래핑하여 던짐")
    fun sendOrderConfirmationEmail_PdfLoadFail() {

        given(orderRepository.findById(100L)).willReturn(Optional.of(order))
        given(paymentRepository.findByOrder_OrderId(100L)).willReturn(Payment(order = order, buyer = user, totalAmount = 0, paymentMethod = PaymentMethod.FREE, paymentStatus = PaymentStatus.DONE, tossOrderId = ""))
        given(javaMailSender.createMimeMessage()).willReturn(mockMimeMessage)

        val dummyHtml = "<html></html>"
        given(resourceLoader.getResource(any())).willReturn(mockResource)
        given(mockResource.inputStream).willReturn(ByteArrayInputStream(dummyHtml.toByteArray()))

        given(fileStorageService.loadFileAsBytes(any())).willAnswer { throw IOException("Disk Read Error") }

        assertThatThrownBy {
            emailService.sendOrderConfirmationEmail(emailDto)
        }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("PDF 파일 로드 실패")

        verify(javaMailSender, never()).send(any<MimeMessage>())
    }
    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}