package com.mysite.knitly.domain.payment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
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
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.email.entity.EmailOutbox
import com.mysite.knitly.global.email.repository.EmailOutboxRepository
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var redisProductService: RedisProductService

    @Mock
    private lateinit var tossApiClient: TossApiClient

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var emailOutboxRepository: EmailOutboxRepository

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setUp() {
        paymentService = PaymentService(
            orderRepository = orderRepository,
            paymentRepository = paymentRepository,
            redisProductService = redisProductService,
            tossApiClient = tossApiClient,
            objectMapper = objectMapper,
            emailOutboxRepository = emailOutboxRepository
        )
    }

    @Test
    @DisplayName("결제 승인 - 정상")
    fun confirmPayment_success() {
        // given
        val user = testUser(1L)
        val design = testDesign(1L, user)
        val product = testProduct(1L, "도안1", 10000.0, user, design)
        val order = testOrder(1L, user, "ORDER123", 10000.0)
        val orderItem = testOrderItem(order, product, 1, 10000.0)

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = null,
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.READY,
            requestedAt = LocalDateTime.now()
        )

        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key_123",
            orderId = "ORDER123",
            amount = 10000L
        )

        val tossResponse = createTossResponse("DONE", "CARD")

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }
        whenever(tossApiClient.confirmPayment(request)).thenReturn(tossResponse)
        doNothing().whenever(redisProductService).incrementPurchaseCount(any())
        doNothing().whenever(redisProductService).evictPopularListCache()

        // EmailOutboxRepository 모킹 수정
        whenever(emailOutboxRepository.save(any<EmailOutbox>())).thenAnswer { invocation ->
            val outbox = invocation.arguments[0] as EmailOutbox
            // 리플렉션으로 ID 설정 (테스트 환경에서 ID 생성 시뮬레이션)
            try {
                val idField = EmailOutbox::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(outbox, 100L)
            } catch (e: Exception) {
                throw RuntimeException("EmailOutbox ID 설정 실패", e)
            }
            outbox // 반드시 입력받은 객체를 반환해야 함 (null 반환 시 NPE 발생)
        }
        whenever(objectMapper.writeValueAsString(any())).thenReturn("""{"dummy":"json"}""")

        // when
        val response = paymentService.confirmPayment(request)

        // then
        assertThat(response).isNotNull
        assertThat(response.paymentKey).isEqualTo("test_payment_key_123")
        assertThat(response.status).isEqualTo(PaymentStatus.DONE)
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.DONE)
        assertThat(payment.tossPaymentKey).isEqualTo("test_payment_key_123")

        verify(tossApiClient).confirmPayment(request)
        verify(paymentRepository, times(2)).save(any<Payment>())
        verify(redisProductService).incrementPurchaseCount(1L)
        verify(redisProductService).evictPopularListCache()
        verify(emailOutboxRepository).save(any<EmailOutbox>())
    }

    @Test
    @DisplayName("결제 승인 - 주문을 찾을 수 없음")
    fun confirmPayment_orderNotFound() {
        // given
        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key",
            orderId = "ORDER123",
            amount = 10000L
        )

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(null)

        // when & then
        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_FOUND)

        verifyNoInteractions(tossApiClient, redisProductService)
    }

    @Test
    @DisplayName("결제 승인 - 금액 불일치")
    fun confirmPayment_amountMismatch() {
        // given
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123", 10000.0)

        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key",
            orderId = "ORDER123",
            amount = 20000L  // 주문 금액과 다름
        )

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)

        // when & then
        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH)

        verifyNoInteractions(tossApiClient, redisProductService)
    }

    @Test
    @DisplayName("결제 승인 - Payment가 존재하지 않음")
    fun confirmPayment_paymentNotFound() {
        // given
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123", 10000.0)

        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key",
            orderId = "ORDER123",
            amount = 10000L
        )

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(null)

        // when & then
        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_FOUND)

        verifyNoInteractions(tossApiClient, redisProductService)
    }

    @Test
    @DisplayName("결제 승인 - 이미 완료된 결제")
    fun confirmPayment_alreadyDone() {
        // given
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123", 10000.0)

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "existing_key",
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.DONE,  // 이미 완료됨
            requestedAt = LocalDateTime.now()
        )

        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key",
            orderId = "ORDER123",
            amount = 10000L
        )

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)

        // when & then
        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_ALREADY_EXISTS)

        verifyNoInteractions(tossApiClient, redisProductService)
    }

    @Test
    @DisplayName("결제 승인 - 토스 API 호출 실패 (IOException)")
    fun confirmPayment_tossApiFailure() {
        // given
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123", 10000.0)

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = null,
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.READY,
            requestedAt = LocalDateTime.now()
        )

        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key",
            orderId = "ORDER123",
            amount = 10000L
        )

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }
        whenever(tossApiClient.confirmPayment(request)).thenThrow(IOException("API 호출 실패"))

        // when & then
        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_API_CALL_FAILED)

        // Payment가 FAILED로 변경되었는지 확인
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.FAILED)
        verify(paymentRepository, atLeast(1)).save(payment)
        verifyNoInteractions(redisProductService)
    }

    @Test
    @DisplayName("결제 취소 - 정상")
    fun cancelPayment_success() {
        // given
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123", 10000.0)

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "test_payment_key",
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.DONE,
            requestedAt = LocalDateTime.now(),
            approvedAt = LocalDateTime.now()
        )

        val request = PaymentCancelRequest(cancelReason = "단순 변심")

        whenever(paymentRepository.findById(1L)).thenReturn(Optional.of(payment))
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }
        whenever(tossApiClient.cancelPayment("test_payment_key", "단순 변심"))
            .thenReturn(
                JsonNodeFactory.instance.objectNode().apply {
                    put("status", "CANCELED")
                    put("cancelReason", "단순 변심")
                    put("canceledAt", LocalDateTime.now().toString())
                }
            )
        // when
        val response = paymentService.cancelPayment(1L, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.paymentId).isEqualTo(1L)
        assertThat(response.status).isEqualTo(PaymentStatus.CANCELED)
        assertThat(response.cancelReason).isEqualTo("단순 변심")
        assertThat(response.canceledAt).isNotNull  // ✅ 수정: canceledAt 검증
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.CANCELED)
        assertThat(payment.canceledAt).isNotNull  // ✅ 추가: payment의 canceledAt 검증

        verify(tossApiClient).cancelPayment("test_payment_key", "단순 변심")
        verify(paymentRepository).save(payment)
    }

    @Test
    @DisplayName("결제 취소 - 취소 불가능한 상태")
    fun cancelPayment_notCancelable() {
        // given
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123", 10000.0)

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "test_payment_key",
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.FAILED,  // 취소 불가능한 상태
            requestedAt = LocalDateTime.now()
        )

        val request = PaymentCancelRequest(cancelReason = "단순 변심")

        whenever(paymentRepository.findById(1L)).thenReturn(Optional.of(payment))

        // when & then
        assertThatThrownBy { paymentService.cancelPayment(1L, request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_CANCELABLE)

        verifyNoInteractions(tossApiClient)
    }

    @Test
    @DisplayName("결제 상태 조회 - 정상")
    fun queryPaymentStatus_success() {
        // given
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123", 10000.0)

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "test_payment_key",
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.IN_PROGRESS,
            requestedAt = LocalDateTime.now()
        )

        val tossResponse = createTossResponse("DONE", "CARD")

        whenever(paymentRepository.findById(1L)).thenReturn(Optional.of(payment))
        whenever(tossApiClient.queryPayment("test_payment_key")).thenReturn(tossResponse)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }

        // when
        val status = paymentService.queryPaymentStatus(1L)

        // then
        assertThat(status).isEqualTo(PaymentStatus.DONE)
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.DONE)
        verify(paymentRepository).save(payment)
    }

    @Test
    @DisplayName("상품 인기도 증가 - DB와 Redis 모두 증가")
    fun incrementProductPopularity_success() {
        // given
        val user = testUser(1L)
        val design1 = testDesign(1L, user)
        val design2 = testDesign(2L, user)
        val product1 = testProduct(1L, "도안1", 10000.0, user, design1)
        val product2 = testProduct(2L, "도안2", 20000.0, user, design2)

        val order = testOrder(1L, user, "ORDER123", 40000.0)
        val orderItem1 = testOrderItem(order, product1, 2, 20000.0)  // 수량 2
        val orderItem2 = testOrderItem(order, product2, 1, 20000.0)  // 수량 1

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = null,
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 40000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.READY,
            requestedAt = LocalDateTime.now()
        )

        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key",
            orderId = "ORDER123",
            amount = 40000L
        )

        val tossResponse = createTossResponse("DONE", "CARD")

        whenever(emailOutboxRepository.save(any<EmailOutbox>())).thenAnswer { invocation ->
            val outbox = invocation.arguments[0] as EmailOutbox
            try {
                val idField = EmailOutbox::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(outbox, 100L)
            } catch (ignored: Exception) { }
            outbox
        }
        whenever(objectMapper.writeValueAsString(any<Any>())).thenReturn("""{"ok":true}""")

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }
        whenever(tossApiClient.confirmPayment(request)).thenReturn(tossResponse)
        doNothing().whenever(redisProductService).incrementPurchaseCount(any<Long>())
        doNothing().whenever(redisProductService).evictPopularListCache()

        val initialPurchaseCount1 = product1.purchaseCount
        val initialPurchaseCount2 = product2.purchaseCount

        // when
        paymentService.confirmPayment(request)

        // then
        // DB에서 purchaseCount 증가 확인
        assertThat(product1.purchaseCount).isEqualTo(initialPurchaseCount1 + 2)
        assertThat(product2.purchaseCount).isEqualTo(initialPurchaseCount2 + 1)

        // Redis 증가 호출 확인 (수량만큼)
        verify(redisProductService, times(2)).incrementPurchaseCount(1L)
        verify(redisProductService, times(1)).incrementPurchaseCount(2L)
        verify(redisProductService).evictPopularListCache()
    }

    // Helper 메서드들
    private fun testUser(id: Long): User =
        User.builder()
            .userId(id)
            .name("유저$id")
            .email("user$id@test.com")
            .provider(Provider.GOOGLE)
            .build()

    private fun testDesign(id: Long, user: User): Design =
        Design(
            designId = id,
            user = user,
            pdfUrl = "/files/design$id.pdf",
            designState = DesignState.ON_SALE,
            designType = null,
            designName = "도안$id",
            gridData = "[]"
        )

    private fun testProduct(id: Long, title: String, price: Double, user: User, design: Design): Product =
        Product(
            productId = id,
            title = title,
            description = "상품 설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "FREE",
            price = price,
            user = user,
            purchaseCount = 0,
            isDeleted = false,
            stockQuantity = null,
            likeCount = 0,
            design = design
        )

    private fun testOrder(id: Long, user: User, tossOrderId: String, totalPrice: Double): Order {
        val order = Order(
            user = user,
            totalPrice = totalPrice,
            tossOrderId = tossOrderId
        )

        // 리플렉션으로 orderId 설정
        try {
            // Order 클래스의 필드 확인 (보통 "orderId" 또는 "id")
            val idField = Order::class.java.getDeclaredField("orderId")
            idField.isAccessible = true
            idField.set(order, id)
        } catch (e: NoSuchFieldException) {
            // 필드명이 다를 경우를 대비
            try {
                val idField = Order::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(order, id)
            } catch (e2: Exception) {
                throw RuntimeException("Order ID 설정 실패: 필드명을 찾을 수 없습니다.", e2)
            }
        } catch (e: Exception) {
            throw RuntimeException("Order ID 설정 중 예외 발생", e)
        }

        return order
    }

    private fun testOrderItem(order: Order, product: Product, quantity: Int, orderPrice: Double): OrderItem {
        val orderItem = OrderItem(
            order = order,
            product = product,
            orderPrice = orderPrice,
            quantity = quantity
        )

        order.orderItems.add(orderItem)

        return orderItem
    }

    private fun createTossResponse(status: String, method: String): JsonNode {
        val factory = JsonNodeFactory.instance
        return factory.objectNode().apply {
            put("status", status)
            put("method", method)
            put("orderName", "테스트 주문")
            put("mId", "test_mid")
            put("approvedAt", LocalDateTime.now().toString())
        }
    }
}