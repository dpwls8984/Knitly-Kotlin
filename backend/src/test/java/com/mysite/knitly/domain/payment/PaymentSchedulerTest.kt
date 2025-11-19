package com.mysite.knitly.domain.payment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.payment.client.TossApiClient
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.payment.scheduler.PaymentScheduler
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PaymentSchedulerTest {

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var tossApiClient: TossApiClient

    private lateinit var paymentScheduler: PaymentScheduler

    @BeforeEach
    fun setUp() {
        paymentScheduler = PaymentScheduler(
            paymentRepository = paymentRepository,
            tossApiClient = tossApiClient
        )
    }

    @Test
    @DisplayName("IN_PROGRESS 결제 복구 - 토스에서 DONE 상태로 복구")
    fun reconcilePayments_inProgressToDone() {
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
            requestedAt = LocalDateTime.now().minusMinutes(15)  // 15분 전
        )

        val tossResponse = createTossResponse("DONE", "CARD")

        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS),
                any()
            )
        ).thenReturn(listOf(payment))
        whenever(tossApiClient.queryPayment("test_payment_key")).thenReturn(tossResponse)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }

        // when
        paymentScheduler.reconcilePayments()

        // then
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.DONE)
        assertThat(payment.approvedAt).isNotNull
        verify(tossApiClient).queryPayment("test_payment_key")
        verify(paymentRepository).save(payment)
    }

    @Test
    @DisplayName("IN_PROGRESS 결제 복구 - 토스에서 FAILED 상태로 복구")
    fun reconcilePayments_inProgressToFailed() {
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
            requestedAt = LocalDateTime.now().minusMinutes(15)
        )

        val tossResponse = createTossResponse("FAILED", "CARD")

        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS),
                any()
            )
        ).thenReturn(listOf(payment))
        whenever(tossApiClient.queryPayment("test_payment_key")).thenReturn(tossResponse)
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }

        // when
        paymentScheduler.reconcilePayments()

        // then
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment.failureReason).contains("토스 상태: FAILED")
        verify(paymentRepository).save(payment)
    }

    @Test
    @DisplayName("IN_PROGRESS 결제 복구 - 토스에서도 IN_PROGRESS면 복구 안함")
    fun reconcilePayments_stillInProgress() {
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
            requestedAt = LocalDateTime.now().minusMinutes(15)
        )

        val tossResponse = createTossResponse("IN_PROGRESS", "CARD")

        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS),
                any()
            )
        ).thenReturn(listOf(payment))
        whenever(tossApiClient.queryPayment("test_payment_key")).thenReturn(tossResponse)

        // when
        paymentScheduler.reconcilePayments()

        // then
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.IN_PROGRESS)  // 변경 없음
        verify(paymentRepository, never()).save(payment)
    }

    @Test
    @DisplayName("IN_PROGRESS 결제 복구 - 복구 대상 없음")
    fun reconcilePayments_noInProgressPayments() {
        // given
        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS),
                any()
            )
        ).thenReturn(emptyList())

        // when
        paymentScheduler.reconcilePayments()

        // then
        verifyNoInteractions(tossApiClient)
        verify(paymentRepository, never()).save(any())
    }

    @Test
    @DisplayName("READY 결제 취소 - 30분 경과 시 자동 취소")
    fun reconcilePayments_cancelAbandonedReady() {
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
            requestedAt = LocalDateTime.now().minusMinutes(35)  // 35분 전
        )

        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS),
                any()
            )
        ).thenReturn(emptyList())
        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.READY),
                any()
            )
        ).thenReturn(listOf(payment))
        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }

        // when
        paymentScheduler.reconcilePayments()

        // then
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.CANCELED)
        assertThat(payment.cancelReason).isEqualTo("결제 위젯에서 30분간 미진행")
        assertThat(payment.canceledAt).isNotNull
        verify(paymentRepository).save(payment)
    }

    @Test
    @DisplayName("READY 결제 취소 - 취소 대상 없음")
    fun reconcilePayments_noReadyPayments() {
        // given
        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS),
                any()
            )
        ).thenReturn(emptyList())
        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.READY),
                any()
            )
        ).thenReturn(emptyList())

        // when
        paymentScheduler.reconcilePayments()

        // then
        verify(paymentRepository, never()).save(any())
    }

    @Test
    @DisplayName("결제 복구 - 여러 건 동시 처리")
    fun reconcilePayments_multiplePayments() {
        // given
        val user = testUser(1L)
        val order1 = testOrder(1L, user, "ORDER1", 10000.0)
        val order2 = testOrder(2L, user, "ORDER2", 20000.0)
        val order3 = testOrder(3L, user, "ORDER3", 30000.0)

        val payment1 = Payment(
            paymentId = 1L,
            tossPaymentKey = "key1",
            tossOrderId = "ORDER1",
            order = order1,
            buyer = user,
            totalAmount = 10000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.IN_PROGRESS,
            requestedAt = LocalDateTime.now().minusMinutes(15)
        )

        val payment2 = Payment(
            paymentId = 2L,
            tossPaymentKey = "key2",
            tossOrderId = "ORDER2",
            order = order2,
            buyer = user,
            totalAmount = 20000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.IN_PROGRESS,
            requestedAt = LocalDateTime.now().minusMinutes(15)
        )

        val payment3 = Payment(
            paymentId = 3L,
            tossPaymentKey = "key3",
            tossOrderId = "ORDER3",
            order = order3,
            buyer = user,
            totalAmount = 30000L,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.IN_PROGRESS,
            requestedAt = LocalDateTime.now().minusMinutes(15)
        )

        whenever(
            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS),
                any()
            )
        ).thenReturn(listOf(payment1, payment2, payment3))

        whenever(tossApiClient.queryPayment("key1")).thenReturn(createTossResponse("DONE", "CARD"))
        whenever(tossApiClient.queryPayment("key2")).thenReturn(createTossResponse("FAILED", "CARD"))
        whenever(tossApiClient.queryPayment("key3")).thenReturn(createTossResponse("IN_PROGRESS", "CARD"))

        whenever(paymentRepository.save(any<Payment>())).thenAnswer { it.arguments[0] as Payment }

        // when
        paymentScheduler.reconcilePayments()

        // then
        assertThat(payment1.paymentStatus).isEqualTo(PaymentStatus.DONE)
        assertThat(payment2.paymentStatus).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment3.paymentStatus).isEqualTo(PaymentStatus.IN_PROGRESS)

        verify(paymentRepository, times(2)).save(any<Payment>())  // payment1, payment2만 저장
    }

    // Helper 메서드들
    private fun testUser(id: Long): User =
        User.builder()
            .userId(id)
            .name("유저$id")
            .email("user$id@test.com")
            .provider(Provider.GOOGLE)
            .build()

    private fun testOrder(id: Long, user: User, tossOrderId: String, totalPrice: Double): Order =
        Order(
            user = user,
            tossOrderId = tossOrderId
        ).apply {
            // orderId를 설정하기 위해 리플렉션 사용
            val field = Order::class.java.getDeclaredField("orderId")
            field.isAccessible = true
            field.set(this, id)
        }

    private fun createTossResponse(status: String, method: String): JsonNode {
        val factory = JsonNodeFactory.instance
        return factory.objectNode().apply {
            put("status", status)
            put("method", method)
            put("approvedAt", LocalDateTime.now().toString())
        }
    }
}