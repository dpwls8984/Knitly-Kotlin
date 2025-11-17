//package com.mysite.knitly.global.email
//
//import com.mysite.knitly.domain.design.entity.Design
//import com.mysite.knitly.domain.order.dto.EmailNotificationDto
//import com.mysite.knitly.domain.order.entity.Order
//import com.mysite.knitly.domain.order.entity.Order.orderId
//import com.mysite.knitly.domain.order.entity.Order.user
//import com.mysite.knitly.domain.order.entity.OrderItem
//import com.mysite.knitly.domain.order.repository.OrderRepository
//import com.mysite.knitly.domain.product.product.entity.Product
//import com.mysite.knitly.domain.product.product.entity.Product.productId
//import com.mysite.knitly.domain.user.entity.User
//import com.mysite.knitly.global.email.service.EmailService
//import com.mysite.knitly.global.util.FileStorageService
//import com.mysite.knitly.utility.handler.OAuth2FailureHandler
//import com.mysite.knitly.utility.handler.OAuth2SuccessHandler
//import jakarta.mail.internet.MimeMessage
//import lombok.extern.slf4j.Slf4j
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//import org.mockito.ArgumentMatchers
//import org.mockito.Mockito
//import org.slf4j.MDC
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.mock.mockito.MockBean
//import org.springframework.mail.javamail.JavaMailSender
//import org.springframework.mail.javamail.JavaMailSenderImpl
//import org.springframework.test.context.ActiveProfiles
//import java.util.*
//import java.util.List
//
//@Slf4j
//@SpringBootTest
//@ActiveProfiles("test")
//internal class EmailNotificationConsumerTest {
//    @Autowired
//    private val emailNotificationConsumer: EmailNotificationConsumer? = null
//
//    @Autowired
//    private val emailService: EmailService? = null
//
//    @MockBean
//    private val javaMailSender: JavaMailSender? = null // 실제 Gmail 발송 방지
//
//    @MockBean
//    private val orderRepository: OrderRepository? = null
//
//    @MockBean
//    private val fileStorageService: FileStorageService? = null // 실제 파일 I/O 방지
//
//    @MockBean
//    private val oAuth2SuccessHandler: OAuth2SuccessHandler? = null
//
//    @MockBean
//    private val oAuth2FailureHandler: OAuth2FailureHandler? = null
//
//    private var emailDto: EmailNotificationDto? = null
//    private var mockOrder: Order? = null
//
//    @BeforeEach
//    fun setUp() {
//        emailDto = EmailNotificationDto(1L, 100L, "customer@knitly.com")
//
//        val mockUser = User.builder().userId(100L).name("김니트").build()
//        val mockDesign = Design.builder().pdfUrl("/fake/path/to/design.pdf").build()
//        val mockProduct: Product = Product.builder().productId(1L).title("테스트 도안").design(mockDesign).build()
//        val mockItem: OrderItem = OrderItem.builder().product(mockProduct).build()
//        mockOrder = Order.builder().orderId(1L).user(mockUser).orderItems(List.of(mockItem)).build()
//
//        MDC.put("testName", "EmailNotificationConsumerTest")
//    }
//
//    @Test
//    @DisplayName("receiveOrderCompletionMessage: 성공 시, JavaMailSender.send가 1회 호출됨")
//    @Throws(
//        Exception::class
//    )
//    fun testReceive_Success() {
//        Mockito.`when`(orderRepository!!.findById(1L)).thenReturn(
//            Optional.of(
//                mockOrder!!
//            )
//        )
//
//        Mockito.`when`(fileStorageService!!.loadFileAsBytes(ArgumentMatchers.anyString()))
//            .thenReturn(byteArrayOf(1, 2, 3))
//
//        Mockito.`when`(javaMailSender!!.createMimeMessage())
//            .thenReturn(JavaMailSenderImpl().createMimeMessage())
//
//        Mockito.doNothing().`when`(javaMailSender).send(ArgumentMatchers.any(MimeMessage::class.java))
//
//        EmailNotificationConsumerTest.log.info("[Test] [When] receiveOrderCompletionMessage 메서드 호출")
//        emailNotificationConsumer!!.receiveOrderCompletionMessage(emailDto!!)
//
//        EmailNotificationConsumerTest.log.info("[Test] [Then] FileStorageService.loadFileAsBytes 검증")
//        Mockito.verify(fileStorageService, Mockito.times(1)).loadFileAsBytes("/fake/path/to/design.pdf")
//
//        // JavaMailSender.send가 1번 호출되었는지 검증
//        EmailNotificationConsumerTest.log.info("[Test] [Then] JavaMailSender.send 검증")
//        Mockito.verify(javaMailSender, Mockito.times(1)).send(
//            ArgumentMatchers.any(
//                MimeMessage::class.java
//            )
//        )
//
//        MDC.clear()
//    }
//
//    @Test
//    @DisplayName("receiveOrderCompletionMessage: Order 조회 실패 시, 예외를 던져 DLQ로 보냄")
//    fun testReceive_OrderNotFound_ShouldThrowException() {
//        EmailNotificationConsumerTest.log.info("[Test] [Given] OrderRepository가 Order를 찾지 못하도록 모킹")
//        Mockito.`when`(orderRepository!!.findById(1L)).thenReturn(Optional.empty())
//
//        EmailNotificationConsumerTest.log.info("[Test] [When] receiveOrderCompletionMessage 메서드 호출")
//
//        // Consumer가 예외를 다시 던져서(re-throw) RabbitMQ가 DLQ 처리하도록 함
//        EmailNotificationConsumerTest.log.info("[Test] [Then] RuntimeException (IllegalArgumentException) 발생 검증")
//        Assertions.assertThrows(RuntimeException::class.java) {
//            emailNotificationConsumer!!.receiveOrderCompletionMessage(emailDto!!)
//        }
//
//        // 예외가 발생했으므로, 메일 발송은 절대 시도되지 않음
//        Mockito.verify(javaMailSender, Mockito.never()).send(
//            ArgumentMatchers.any(
//                MimeMessage::class.java
//            )
//        )
//
//        MDC.clear()
//    }
//}