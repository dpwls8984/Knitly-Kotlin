package com.mysite.knitly.domain.payment

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

    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var paymentRepository: PaymentRepository
    @Mock private lateinit var redisProductService: RedisProductService
    @Mock private lateinit var tossApiClient: TossApiClient
    @Mock private lateinit var objectMapper: ObjectMapper
    @Mock private lateinit var emailOutboxRepository: EmailOutboxRepository

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
        val order = testOrder(1L, user, "ORDER123") // 생성 시점엔 0원
        testOrderItem(order, product, 1, 10000.0)   // 아이템 추가 -> 10000원 됨

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

        val request = PaymentConfirmRequest("test_payment_key_123", "ORDER123", 10000L)

        val tossResponse = JsonNodeFactory.instance.objectNode().apply {
            put("status", "DONE")
            put("method", "CARD")
            put("orderName", "테스트 주문")
            put("mId", "test_mid")
            put("approvedAt", LocalDateTime.now().toString())
        }

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }
        whenever(tossApiClient.confirmPayment(request)).thenReturn(tossResponse)

        doNothing().whenever(redisProductService).incrementPurchaseCount(any())
        doNothing().whenever(redisProductService).evictPopularListCache()
        whenever(emailOutboxRepository.save(any<EmailOutbox>())).thenAnswer { it.arguments[0] }
        whenever(objectMapper.writeValueAsString(any())).thenReturn("""{"dummy":"json"}""")

        // when
        val response = paymentService.confirmPayment(request)

        // then
        assertThat(response.status).isEqualTo(PaymentStatus.DONE)
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.DONE)
        verify(tossApiClient).confirmPayment(request)
    }

    @Test
    @DisplayName("결제 승인 - 주문을 찾을 수 없음")
    fun confirmPayment_orderNotFound() {
        val request = PaymentConfirmRequest("key", "ORDER123", 10000L)
        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(null)

        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_FOUND)
    }

    @Test
    @DisplayName("결제 승인 - 금액 불일치")
    fun confirmPayment_amountMismatch() {
        // given
        val user = testUser(1L)
        val design = testDesign(1L, user)
        val product = testProduct(1L, "도안1", 10000.0, user, design)

        val order = testOrder(1L, user, "ORDER123")
        testOrderItem(order, product, 1, 10000.0) // 주문 총액 10000원 설정

        val request = PaymentConfirmRequest(
            paymentKey = "key",
            orderId = "ORDER123",
            amount = 20000L // 요청은 20000원 -> 불일치 유도
        )

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)

        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
    }

    @Test
    @DisplayName("결제 승인 - Payment가 존재하지 않음")
    fun confirmPayment_paymentNotFound() {
        // given
        val user = testUser(1L)
        val design = testDesign(1L, user)
        val product = testProduct(1L, "도안1", 10000.0, user, design)

        val order = testOrder(1L, user, "ORDER123")
        testOrderItem(order, product, 1, 10000.0) // [수정] 가격 설정 필수 (0원이면 금액 불일치 먼저 뜸)

        val request = PaymentConfirmRequest("key", "ORDER123", 10000L)

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(null)

        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_FOUND)
    }

    @Test
    @DisplayName("결제 승인 - 이미 완료된 결제")
    fun confirmPayment_alreadyDone() {
        // given
        val user = testUser(1L)
        val design = testDesign(1L, user)
        val product = testProduct(1L, "도안1", 10000.0, user, design)

        val order = testOrder(1L, user, "ORDER123")
        testOrderItem(order, product, 1, 10000.0) // [수정] 가격 설정 필수

        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "existing_key",
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.DONE, // 이미 완료됨
            requestedAt = LocalDateTime.now()
        )
        val request = PaymentConfirmRequest("key", "ORDER123", 10000L)

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)

        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_ALREADY_EXISTS)
    }

    @Test
    @DisplayName("결제 승인 - 토스 API 호출 실패")
    fun confirmPayment_tossApiFailure() {
        // given
        val user = testUser(1L)
        val design = testDesign(1L, user)
        val product = testProduct(1L, "도안1", 10000.0, user, design)

        val order = testOrder(1L, user, "ORDER123")
        testOrderItem(order, product, 1, 10000.0) // [수정] 가격 설정 필수

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
        val request = PaymentConfirmRequest("key", "ORDER123", 10000L)

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }

        whenever(tossApiClient.confirmPayment(request)).thenThrow(IOException("API Fail"))

        assertThatThrownBy { paymentService.confirmPayment(request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_API_CALL_FAILED)

        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.FAILED)
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

        val order = testOrder(1L, user, "ORDER123") // 초기 0원
        // 아이템 추가: 20000 + 20000 = 40000원
        testOrderItem(order, product1, 2, 10000.0)
        testOrderItem(order, product2, 1, 20000.0)

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

        val request = PaymentConfirmRequest("test_payment_key", "ORDER123", 40000L)

        val tossResponse = JsonNodeFactory.instance.objectNode().apply {
            put("status", "DONE")
            put("method", "CARD")
            put("approvedAt", LocalDateTime.now().toString())
        }

        whenever(orderRepository.findByTossOrderId("ORDER123")).thenReturn(order)
        whenever(paymentRepository.findByOrder_OrderId(1L)).thenReturn(payment)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }
        whenever(tossApiClient.confirmPayment(request)).thenReturn(tossResponse)
        whenever(emailOutboxRepository.save(any<EmailOutbox>())).thenAnswer { it.arguments[0] }
        whenever(objectMapper.writeValueAsString(any())).thenReturn("{}")

        // when
        paymentService.confirmPayment(request)

        // then
        assertThat(product1.purchaseCount).isEqualTo(2)
        assertThat(product2.purchaseCount).isEqualTo(1)

        verify(redisProductService, times(2)).incrementPurchaseCount(1L)

        // Product2는 수량이 1개이므로, 1번 호출 (times(1)은 생략 가능)
        verify(redisProductService, times(1)).incrementPurchaseCount(2L)

        // 캐시 삭제는 한 번만 일어나면 됨
        verify(redisProductService).evictPopularListCache()
    }

    @Test
    @DisplayName("결제 취소 - 정상")
    fun cancelPayment_success() {
        // 취소 테스트는 Order 가격 검증 로직을 타지 않으므로 OrderItem 추가 불필요하지만
        // 데이터 정합성을 위해 넣어주는 것이 좋음
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123")
        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "key",
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
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] }

        whenever(tossApiClient.cancelPayment(any(), any())).thenReturn(
            JsonNodeFactory.instance.objectNode().apply {
                put("status", "CANCELED")
                put("cancelReason", "단순 변심")
                put("canceledAt", LocalDateTime.now().toString())
            }
        )

        val response = paymentService.cancelPayment(1L, request)
        assertThat(response.status).isEqualTo(PaymentStatus.CANCELED)
    }

    @Test
    @DisplayName("결제 취소 - 취소 불가능한 상태")
    fun cancelPayment_notCancelable() {
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123")
        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "key",
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.FAILED,
            requestedAt = LocalDateTime.now()
        )
        val request = PaymentCancelRequest("변심")

        whenever(paymentRepository.findById(1L)).thenReturn(Optional.of(payment))

        assertThatThrownBy { paymentService.cancelPayment(1L, request) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_CANCELABLE)
    }

    @Test
    @DisplayName("결제 상태 조회 - 정상")
    fun queryPaymentStatus_success() {
        val user = testUser(1L)
        val order = testOrder(1L, user, "ORDER123")
        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "key",
            tossOrderId = "ORDER123",
            order = order,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.IN_PROGRESS,
            requestedAt = LocalDateTime.now()
        )

        whenever(paymentRepository.findById(1L)).thenReturn(Optional.of(payment))
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] }
        whenever(tossApiClient.queryPayment("key")).thenReturn(
            JsonNodeFactory.instance.objectNode().apply {
                put("status", "DONE")
                put("method", "CARD")
                put("approvedAt", LocalDateTime.now().toString())
            }
        )

        val status = paymentService.queryPaymentStatus(1L)
        assertThat(status).isEqualTo(PaymentStatus.DONE)
    }

    // =================================================================================
    // Helper 메서드 수정: totalPrice 리플렉션 제거 -> addOrderItem으로 정상 계산 유도
    // =================================================================================

    private fun testUser(id: Long): User {
        val user = User(
            email = "user$id@test.com",
            name = "유저$id",
            socialId = "social$id",
            provider = Provider.GOOGLE
        )
        val idField = User::class.java.getDeclaredField("userId")
        idField.isAccessible = true
        idField.set(user, id)
        return user
    }

    private fun testDesign(id: Long, user: User): Design {
        return Design(
            user = user,
            designName = "도안$id",
            pdfUrl = "/files/design$id.pdf",
            gridData = "[]",
            designState = DesignState.ON_SALE
        )
    }

    private fun testProduct(id: Long, title: String, price: Double, user: User, design: Design): Product {
        val product = Product(
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
        val idField = Product::class.java.getDeclaredField("productId")
        idField.isAccessible = true
        idField.set(product, id)
        return product
    }

    private fun testOrder(id: Long, user: User, tossOrderId: String): Order {
        // [수정] totalPrice 인자 제거. 생성 직후 가격은 0.0
        val order = Order(
            user = user,
            tossOrderId = tossOrderId
        )
        val idField = Order::class.java.getDeclaredField("orderId")
        idField.isAccessible = true
        idField.set(order, id)

        // [수정] 리플렉션으로 totalPrice 설정하는 부분 삭제.
        // OrderItem을 추가하면 알아서 계산됩니다.
        return order
    }

    private fun testOrderItem(order: Order, product: Product, quantity: Int, orderPrice: Double): OrderItem {
        val orderItem = OrderItem(
            product = product,
            orderPrice = orderPrice,
            quantity = quantity
        )
        // 이 메서드가 호출되어야 Order.totalPrice가 증가함
        order.addOrderItem(orderItem)
        return orderItem
    }
}