package com.mysite.knitly.domain.payment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.client.TossApiClient
import com.mysite.knitly.domain.payment.dto.PaymentCancelRequest
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.payment.service.PaymentService
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var designRepository: DesignRepository

    @Autowired
    private lateinit var redisProductService: RedisProductService

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @MockitoBean
    private lateinit var tossApiClient: TossApiClient

    private lateinit var testUser: User
    private lateinit var testDesign: Design
    private lateinit var testProduct: Product
    private lateinit var testOrder: Order

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = userRepository.save(
            User.builder()
                .name("테스트유저")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .build()
        )

        // 테스트 도안 생성
        testDesign = designRepository.save(
            Design(
                user = testUser,
                designName = "테스트도안",
                pdfUrl = "/files/test.pdf",
                gridData = "{}",
                designState = DesignState.ON_SALE
            )
        )

        // 테스트 상품 생성
        testProduct = productRepository.save(
            Product(
                user = testUser,
                title = "테스트상품",
                description = "테스트 설명",
                productCategory = ProductCategory.BAG,
                price = 10000.0,
                sizeInfo = "Free",
                design = testDesign
            )
        )

        // 테스트 주문 생성
        testOrder = orderRepository.save(
            Order(
                user = testUser,
                totalPrice = 0.0,
                tossOrderId = "ORDER_${System.currentTimeMillis()}"
            )
        )

        // OrderItem 추가
        val orderItem = com.mysite.knitly.domain.order.entity.OrderItem(
            product = testProduct,
            orderPrice = testProduct.price,
            quantity = 1
        )
        testOrder.addOrderItem(orderItem)
        testOrder = orderRepository.save(testOrder)
    }

    @Test
    @DisplayName("결제 승인 - 정상 케이스")
    fun confirmPayment_success() {
        // given
        val tossPaymentKey = "test_payment_key_${System.currentTimeMillis()}"
        val payment = paymentRepository.save(
            Payment(
                tossPaymentKey = tossPaymentKey,
                tossOrderId = testOrder.tossOrderId,
                order = testOrder,
                buyer = testUser,
                totalAmount = 10000L,
                paymentMethod = PaymentMethod.CARD,
                paymentStatus = PaymentStatus.READY
            )
        )

        val tossResponse = createTossResponse("DONE", "CARD", 10000)
        whenever(tossApiClient.confirmPayment(any())).thenReturn(tossResponse)

        val request = PaymentConfirmRequest(
            paymentKey = tossPaymentKey,
            orderId = testOrder.tossOrderId,
            amount = 10000L
        )

        // when
        val response = paymentService.confirmPayment(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(PaymentStatus.DONE)

        val updatedPayment = paymentRepository.findById(payment.paymentId!!).get()
        assertThat(updatedPayment.paymentStatus).isEqualTo(PaymentStatus.DONE)
        assertThat(updatedPayment.approvedAt).isNotNull()
    }

    @Test
    @DisplayName("결제 승인 - 금액 불일치")
    fun confirmPayment_amountMismatch() {
        // given
        val tossPaymentKey = "test_payment_key_${System.currentTimeMillis()}"
        val payment = paymentRepository.save(
            Payment(
                tossPaymentKey = tossPaymentKey,
                tossOrderId = testOrder.tossOrderId,
                order = testOrder,
                buyer = testUser,
                totalAmount = 10000L,
                paymentMethod = PaymentMethod.CARD,
                paymentStatus = PaymentStatus.READY
            )
        )

        val request = PaymentConfirmRequest(
            paymentKey = tossPaymentKey,
            orderId = testOrder.tossOrderId,
            amount = 5000L  // 다른 금액
        )

        // when & then
        try {
            paymentService.confirmPayment(request)
            assert(false) { "예외가 발생해야 함" }
        } catch (e: Exception) {
            // 예외 발생 확인
            val updatedPayment = paymentRepository.findById(payment.paymentId!!).get()
            assertThat(updatedPayment.paymentStatus).isEqualTo(PaymentStatus.FAILED)
        }
    }

    @Test
    @DisplayName("결제 취소 - 정상 케이스")
    fun cancelPayment_success() {
        // given
        val tossPaymentKey = "test_payment_key_${System.currentTimeMillis()}"
        val payment = paymentRepository.save(
            Payment(
                tossPaymentKey = tossPaymentKey,
                tossOrderId = testOrder.tossOrderId,
                order = testOrder,
                buyer = testUser,
                totalAmount = 10000L,
                paymentMethod = PaymentMethod.CARD,
                paymentStatus = PaymentStatus.DONE,
                approvedAt = LocalDateTime.now()
            )
        )

        val tossResponse = createTossResponse("CANCELED", "CARD", 10000)
        whenever(tossApiClient.cancelPayment(any(), any())).thenReturn(tossResponse)

        val request = PaymentCancelRequest(cancelReason = "단순 변심")

        // when
        val response = paymentService.cancelPayment(payment.paymentId!!, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(PaymentStatus.CANCELED)

        val updatedPayment = paymentRepository.findById(payment.paymentId!!).get()
        assertThat(updatedPayment.paymentStatus).isEqualTo(PaymentStatus.CANCELED)
        assertThat(updatedPayment.canceledAt).isNotNull()
        assertThat(updatedPayment.cancelReason).isEqualTo("단순 변심")
    }

    @Test
    @DisplayName("결제 승인 후 Redis 인기도 증가 확인")
    fun confirmPayment_increasesRedisPopularity() {
        // given
        val tossPaymentKey = "test_payment_key_${System.currentTimeMillis()}"
        val payment = paymentRepository.save(
            Payment(
                tossPaymentKey = tossPaymentKey,
                tossOrderId = testOrder.tossOrderId,
                order = testOrder,
                buyer = testUser,
                totalAmount = 10000L,
                paymentMethod = PaymentMethod.CARD,
                paymentStatus = PaymentStatus.READY
            )
        )

        val tossResponse = createTossResponse("DONE", "CARD", 10000)
        whenever(tossApiClient.confirmPayment(any())).thenReturn(tossResponse)

        val request = PaymentConfirmRequest(
            paymentKey = tossPaymentKey,
            orderId = testOrder.tossOrderId,
            amount = 10000L
        )

        // when
        paymentService.confirmPayment(request)

        // then - Redis에 상품이 추가되었는지 확인
        val score = redisTemplate.opsForZSet().score(RedisProductService.POPULAR_KEY, testProduct.productId.toString())
        assertThat(score).isNotNull()
    }

    private fun createTossResponse(status: String, method: String, amount: Long): JsonNode {
        val factory = JsonNodeFactory.instance
        return factory.objectNode().apply {
            put("status", status)
            put("method", method)
            put("totalAmount", amount)
            put("approvedAt", LocalDateTime.now().toString())
            put("mId", "test_mid_123")
        }
    }
}
