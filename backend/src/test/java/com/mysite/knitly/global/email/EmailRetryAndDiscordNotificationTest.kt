package com.mysite.knitly.global.email

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.global.email.service.EmailService
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 이메일 발송 3회 재시도 실패 → Discord 알림 전송 통합 테스트
 *
 * JavaMailSender를 Mock으로 교체하여 항상 실패시킨 뒤,
 * @Retryable 3회 재시도 → @Recover → DiscordNotifier.send() 호출을 검증합니다.
 * Discord 채널에서 실제 메시지 도착을 육안으로 확인하세요.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmailRetryAndDiscordNotificationTest {

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var designRepository: DesignRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @MockBean
    private lateinit var javaMailSender: JavaMailSender

    // Redis 연결 방지
    @MockBean
    private lateinit var redissonClient: RedissonClient

    @MockBean
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private var testOrderId: Long? = null

    @BeforeEach
    fun setUp() {
        // JavaMailSender Mock: createMimeMessage()는 정상 반환, send()는 항상 실패
        val mockMimeMessage = org.mockito.Mockito.mock(MimeMessage::class.java)
        whenever(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage)
        doThrow(RuntimeException("SMTP 강제 실패 - 재시도 테스트용"))
            .whenever(javaMailSender).send(any<MimeMessage>())

        // 테스트 데이터 생성
        val testUser = userRepository.save(
            User(
                email = "test@knitly.com",
                name = "테스트유저",
                socialId = "testSocialId",
                provider = Provider.GOOGLE
            )
        )

        val testDesign = designRepository.save(
            Design(
                user = testUser,
                pdfUrl = "/fake/path/design.pdf",
                designState = DesignState.ON_SALE,
                designName = "테스트 도안",
                gridData = "{}"
            )
        )

        val testProduct = productRepository.save(
            Product(
                title = "테스트 상품",
                description = "테스트용",
                productCategory = ProductCategory.TOP,
                sizeInfo = "Free",
                price = 10000.0,
                user = testUser,
                design = testDesign,
                purchaseCount = 0,
                isDeleted = false,
                stockQuantity = null,
                likeCount = 0
            )
        )

        val testOrder = Order(user = testUser, tossOrderId = UUID.randomUUID().toString())
        testOrder.addOrderItem(OrderItem(product = testProduct, orderPrice = 10000.0, quantity = 1))
        orderRepository.save(testOrder)
        this.testOrderId = testOrder.orderId

        paymentRepository.save(
            Payment(
                order = testOrder,
                buyer = testUser,
                tossPaymentKey = "test-key",
                tossOrderId = testOrder.tossOrderId,
                totalAmount = 10000L,
                paymentMethod = PaymentMethod.CARD,
                paymentStatus = PaymentStatus.DONE
            )
        )
    }

    @Test
    @DisplayName("이메일 3회 재시도 실패 후 Discord 알림이 전송된다")
    fun emailRetryFailure_sendsDiscordNotification() {
        val emailDto = EmailNotificationDto(
            orderId = testOrderId!!,
            userId = 1L,
            userEmail = "test@knitly.com"
        )

        // @Async로 비동기 실행되므로 호출 자체는 즉시 반환됨
        emailService.sendOrderConfirmationEmail(emailDto)

        // 3회 재시도 (2초 + 4초 + 8초) + 여유 시간 대기
        // @Retryable backoff: delay=2000, multiplier=2.0 → 2s, 4s, 8s = 총 ~14초
        println("========================================")
        println("[테스트] 이메일 3회 재시도 대기 중... (약 16초)")
        println("[테스트] Discord 채널을 확인하세요!")
        println("========================================")

        TimeUnit.SECONDS.sleep(16)

        println("========================================")
        println("[테스트] 대기 완료. Discord 채널에 알림이 도착했는지 확인하세요.")
        println("========================================")
    }
}
