package com.mysite.knitly.domain.payment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    @InjectMocks
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var redisProductService: RedisProductService

    @Mock
    private lateinit var tossApiClient: TossApiClient

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun init() {
    }

    @DisplayName("결제 승인 성공 시 READY → IN_PROGRESS → DONE 상태로 저장")
    @Test
    fun `confirmPayment success DONE`() {
        // given
        val orderId = "ORD-1"
        val paymentKey = "pay_1"
        val amount = 15000L

        val order = stubOrder(orderId, amount)
        val ready = stubPayment(order, PaymentStatus.READY, null)

        `when`(orderRepository.findByTossOrderId(orderId)).thenReturn(order)
        `when`(paymentRepository.findByOrder_OrderId(order.orderId)).thenReturn(ready)
        `when`(paymentRepository.save(any())).thenAnswer { it.getArgument(0) }

        val toss = objectMapper.readTree(
            """
            {
              "status":"DONE",
              "method":"CARD",
              "mId":"mid_x",
              "approvedAt":"2025-11-10T03:10:00Z",
              "orderName":"주문명",
              "totalAmount":15000,
              "card":{"company":"Hyundai","number":"1234-****","approveNo":"9999"}
            }
            """.trimIndent()
        )
        `when`(tossApiClient.confirmPayment(any(PaymentConfirmRequest::class.java))).thenReturn(toss)

        val req = PaymentConfirmRequest(paymentKey, orderId, amount)

        // when
        val res = paymentService.confirmPayment(req)

        // then
        assertEquals(PaymentStatus.DONE, res.status)
        assertEquals(paymentKey, res.paymentKey)
        assertEquals(orderId, res.orderId)
        // READY -> IN_PROGRESS 저장 + DONE 저장 최소 2회
        verify(paymentRepository, atLeast(2)).save(any(Payment::class.java))
    }

    @DisplayName("결제 조회 시 IN_PROGRESS → DONE으로 동기화")
    @Test
    fun `queryPaymentStatus syncFromInProgressToDone`() {
        // given
        val payment = Payment(
            paymentId = 1L,
            tossPaymentKey = "pay_q",
            tossOrderId = "",
            order = Order(),
            buyer = User(),
            totalAmount = 0L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.IN_PROGRESS
        )

        `when`(paymentRepository.findById(1L)).thenReturn(Optional.of(payment))
        `when`(paymentRepository.save(any())).thenAnswer { it.getArgument(0) }

        val toss = objectMapper.readTree(
            """
            {"status":"DONE","method":"CARD","approvedAt":"2025-11-10T03:10:00Z"}
            """.trimIndent()
        )
        `when`(tossApiClient.queryPayment("pay_q")).thenReturn(toss)

        // when
        val result = paymentService.queryPaymentStatus(1L)

        // then
        assertEquals(PaymentStatus.DONE, result)
        verify(paymentRepository).save(any(Payment::class.java))
    }

    @DisplayName("결제 취소 요청 시 CANCELED 상태로 업데이트")
    @Test
    fun `cancelPayment success`() {
        // given
        val payment = Payment(
            paymentId = 10L,
            tossPaymentKey = "pay_c",
            tossOrderId = "",
            order = Order(),
            buyer = User(),
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.DONE
        )

        `when`(paymentRepository.findById(10L)).thenReturn(Optional.of(payment))
        `when`(paymentRepository.save(any())).thenAnswer { it.getArgument(0) }

        val tossCancel = objectMapper.readTree(
            """
            {"status":"CANCELED"}
            """.trimIndent()
        )
        `when`(tossApiClient.cancelPayment(eq("pay_c"), anyString())).thenReturn(tossCancel)

        // when
        val res = paymentService.cancelPayment(10L, PaymentCancelRequest("사용자 취소"))

        // then
        assertEquals(PaymentStatus.CANCELED, res.status)
        verify(paymentRepository).save(any(Payment::class.java))
    }

    @DisplayName("웹훅 수신 시 Payment가 없으면 토스 API를 통해 동기화 후 저장")
    @Test
    fun `handleWebhook syncWhenPaymentMissing`() {
        // given
        val payload = mapOf(
            "eventType" to "PAYMENT_APPROVED",
            "data" to mapOf(
                "paymentKey" to "pay_w",
                "status" to "DONE",
                "approvedAt" to "2025-11-10T03:10:00Z"
            )
        )

        `when`(paymentRepository.findByTossPaymentKey("pay_w")).thenReturn(null)

        // 토스 조회 → 동기화
        val order = stubOrder("ORD-W", 5000L)
        `when`(orderRepository.findByTossOrderId("ORD-W")).thenReturn(order)
        `when`(paymentRepository.save(any())).thenAnswer { it.getArgument(0) }

        val toss = objectMapper.readTree(
            """
            { "paymentKey":"pay_w","status":"DONE","method":"CARD","orderId":"ORD-W","totalAmount":5000,
              "approvedAt":"2025-11-10T03:10:00Z" }
            """.trimIndent()
        )
        `when`(tossApiClient.queryPayment("pay_w")).thenReturn(toss)

        // when
        paymentService.handleWebhook(payload)

        // then
        verify(paymentRepository, atLeastOnce()).save(any(Payment::class.java)) // sync 저장
    }

    private fun stubOrder(tossOrderId: String, amount: Long): Order {
        val buyer = User(
            userId = 1L,
            name = "buyer",
            email = "buyer@test.com",
            password = "password"
        )

        return Order(
            orderId = 100L,
            tossOrderId = tossOrderId,
            user = buyer,
            totalPrice = amount.toDouble()
        )
    }

    private fun stubPayment(order: Order, status: PaymentStatus, paymentKey: String?): Payment {
        return Payment(
            paymentId = 200L,
            order = order,
            buyer = order.user,
            paymentStatus = status,
            tossOrderId = order.tossOrderId,
            tossPaymentKey = paymentKey,
            totalAmount = order.totalPrice.toLong(),
            paymentMethod = PaymentMethod.CARD,
            requestedAt = LocalDateTime.now().minusMinutes(15)
        )
    }
}