package com.mysite.knitly.global.email

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.email.service.EmailService
import com.mysite.knitly.global.util.FileStorageService
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.ActiveProfiles
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class EmailNotificationConsumerTest {

    @Autowired
    private lateinit var emailNotificationConsumer: EmailNotificationConsumer

    @MockBean
    private lateinit var javaMailSender: JavaMailSender

    @MockBean
    private lateinit var orderRepository: OrderRepository

    @MockBean
    private lateinit var fileStorageService: FileStorageService

    // EmailService는 실제 로직을 테스트하기 위해 Mock하지 않음 (통합 테스트 성격)
    // 만약 단위 테스트로 변경하고 싶다면 @MockBean으로 변경해야 함

    private lateinit var emailDto: EmailNotificationDto
    private lateinit var mockOrder: Order

    @BeforeEach
    fun setUp() {
        emailDto = EmailNotificationDto(1L, 100L, "customer@knitly.com")

        val mockUser = User(userId = 100L, name = "김니트", email = "customer@knitly.com", socialId = "test", provider = com.mysite.knitly.domain.user.entity.Provider.GOOGLE)
        val mockDesign = Design(pdfUrl = "/fake/path/to/design.pdf", user = mockUser, designState = com.mysite.knitly.domain.design.entity.DesignState.ON_SALE, designName = "테스트 도안", gridData = "{}")
        val mockProduct = Product(productId = 1L, title = "테스트 도안", design = mockDesign, user = mockUser, description = "", productCategory = com.mysite.knitly.domain.product.product.entity.ProductCategory.ETC, sizeInfo = "", price = 0.0)
        val initialOrder = Order(user = mockUser, totalPrice = 0.0, tossOrderId = "test-order-toss")
        val mockItem = OrderItem(
            product = mockProduct,
            orderPrice = 0.0,
            quantity = 1
        )
        initialOrder.addOrderItem(mockItem) // OrderItems에 추가 및 totalPrice 업데이트
        val createdAtField: Field = Order::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(initialOrder, LocalDateTime.now())

        mockOrder = initialOrder
        val idField = Order::class.java.getDeclaredField("orderId")
        idField.isAccessible = true
        idField.set(mockOrder, 1L)
    }

    @Test
    @DisplayName("receiveOrderCompletionMessage: 성공 시, JavaMailSender.send가 1회 호출됨")
    fun testReceive_Success() {
        given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder))
        given(fileStorageService.loadFileAsBytes(any())).willReturn(byteArrayOf(1, 2, 3))
        given(javaMailSender.createMimeMessage()).willReturn(JavaMailSenderImpl().createMimeMessage())

        // doNothing()은 void 메서드에 대해 기본값이므로 생략 가능하지만 명시적으로 작성
        doNothing().`when`(javaMailSender).send(any<MimeMessage>())

        emailNotificationConsumer.receiveOrderCompletionMessage(emailDto)

        verify(fileStorageService, times(1)).loadFileAsBytes("/fake/path/to/design.pdf")
        verify(javaMailSender, times(1)).send(any<MimeMessage>())
    }

    @Test
    @DisplayName("receiveOrderCompletionMessage: Order 조회 실패 시, 예외를 던져 DLQ로 보냄")
    fun testReceive_OrderNotFound_ShouldThrowException() {
        given(orderRepository.findById(1L)).willReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            emailNotificationConsumer.receiveOrderCompletionMessage(emailDto)
        }

        verify(javaMailSender, never()).send(any<MimeMessage>())
    }
}