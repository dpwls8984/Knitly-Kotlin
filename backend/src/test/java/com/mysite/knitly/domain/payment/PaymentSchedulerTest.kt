//package com.mysite.knitly.domain.payment
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.mysite.knitly.domain.order.entity.Order
//import com.mysite.knitly.domain.payment.client.TossApiClient
//import com.mysite.knitly.domain.payment.entity.Payment
//import com.mysite.knitly.domain.payment.entity.PaymentMethod
//import com.mysite.knitly.domain.payment.entity.PaymentStatus
//import com.mysite.knitly.domain.payment.repository.PaymentRepository
//import com.mysite.knitly.domain.payment.scheduler.PaymentScheduler
//import com.mysite.knitly.domain.user.entity.User
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.ExtendWith
//import org.mockito.InjectMocks
//import org.mockito.Mock
//import org.mockito.Mockito.*
//import org.mockito.junit.jupiter.MockitoExtension
//import java.time.LocalDateTime
//
//@ExtendWith(MockitoExtension::class)
//class PaymentSchedulerTest {
//
//    @InjectMocks
//    private lateinit var paymentScheduler: PaymentScheduler
//
//    @Mock
//    private lateinit var paymentRepository: PaymentRepository
//
//    @Mock
//    private lateinit var tossApiClient: TossApiClient
//
//    private val om = ObjectMapper()
//
//    @Test
//    fun `reconcilePayments inProgressToDone and readyToCanceled`() {
//        // ----- IN_PROGRESS 복구 대상 -----
//        val inProgress = Payment(
//            paymentId = 1L,
//            tossPaymentKey = "pay_1",
//            tossOrderId = "",
//            order = Order(),
//            buyer = User(),
//            totalAmount = 12345L,
//            paymentMethod = PaymentMethod.CARD,
//            paymentStatus = PaymentStatus.IN_PROGRESS,
//            requestedAt = LocalDateTime.now().minusMinutes(20)
//        )
//
//        `when`(
//            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
//                eq(PaymentStatus.IN_PROGRESS),
//                any(LocalDateTime::class.java)
//            )
//        ).thenReturn(listOf(inProgress))
//
//        val done = om.readTree(
//            """
//            {"status":"DONE","method":"CARD","approvedAt":"2025-11-10T03:10:00Z"}
//            """.trimIndent()
//        )
//        `when`(tossApiClient.queryPayment("pay_1")).thenReturn(done)
//
//        // ----- READY 취소 대상 -----
//        val ready = Payment(
//            paymentId = 2L,
//            tossPaymentKey = null,
//            tossOrderId = "",
//            order = Order(),
//            buyer = User(),
//            totalAmount = 0L,
//            paymentMethod = PaymentMethod.CARD,
//            paymentStatus = PaymentStatus.READY,
//            requestedAt = LocalDateTime.now().minusMinutes(40)
//        )
//
//        `when`(
//            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
//                eq(PaymentStatus.READY),
//                any(LocalDateTime::class.java)
//            )
//        ).thenReturn(listOf(ready))
//
//        `when`(paymentRepository.save(any())).thenAnswer { it.getArgument(0) }
//
//        // when
//        paymentScheduler.reconcilePayments()
//
//        // then
//        // IN_PROGRESS → DONE 저장, READY → CANCELED 저장
//        verify(paymentRepository, atLeast(2)).save(any(Payment::class.java))
//        assertEquals(PaymentStatus.DONE, inProgress.paymentStatus)
//        assertEquals(PaymentStatus.CANCELED, ready.paymentStatus)
//    }
//
//    @Test
//    fun `reconcilePayments inProgressCanceledOnToss`() {
//        // given
//        val inProgress = Payment(
//            paymentId = 3L,
//            tossPaymentKey = "pay_3",
//            tossOrderId = "",
//            order = Order(),
//            buyer = User(),
//            totalAmount = 0L,
//            paymentMethod = PaymentMethod.CARD,
//            paymentStatus = PaymentStatus.IN_PROGRESS,
//            requestedAt = LocalDateTime.now().minusMinutes(30)
//        )
//
//        `when`(
//            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
//                eq(PaymentStatus.IN_PROGRESS),
//                any(LocalDateTime::class.java)
//            )
//        ).thenReturn(listOf(inProgress))
//
//        val canceled = om.readTree(
//            """
//            {"status":"CANCELED"}
//            """.trimIndent()
//        )
//        `when`(tossApiClient.queryPayment("pay_3")).thenReturn(canceled)
//        `when`(
//            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
//                eq(PaymentStatus.READY),
//                any(LocalDateTime::class.java)
//            )
//        ).thenReturn(emptyList()) // READY 없음
//
//        `when`(paymentRepository.save(any())).thenAnswer { it.getArgument(0) }
//
//        // when
//        paymentScheduler.reconcilePayments()
//
//        // then
//        assertEquals(PaymentStatus.FAILED, inProgress.paymentStatus) // payment.fail(...) 도메인에서 FAILED로 바뀐다고 가정
//        verify(paymentRepository).save(any(Payment::class.java))
//    }
//
//    @Test
//    fun `reconcilePayments inProgressStillInProgress`() {
//        // given
//        val inProgress = Payment(
//            paymentId = 4L,
//            tossPaymentKey = "pay_4",
//            tossOrderId = "",
//            order = Order(),
//            buyer = User(),
//            totalAmount = 0L,
//            paymentMethod = PaymentMethod.CARD,
//            paymentStatus = PaymentStatus.IN_PROGRESS,
//            requestedAt = LocalDateTime.now().minusMinutes(25)
//        )
//
//        `when`(
//            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
//                eq(PaymentStatus.IN_PROGRESS),
//                any(LocalDateTime::class.java)
//            )
//        ).thenReturn(listOf(inProgress))
//
//        val still = om.readTree(
//            """
//            {"status":"IN_PROGRESS"}
//            """.trimIndent()
//        )
//        `when`(tossApiClient.queryPayment("pay_4")).thenReturn(still)
//        `when`(
//            paymentRepository.findByPaymentStatusAndRequestedAtBefore(
//                eq(PaymentStatus.READY),
//                any(LocalDateTime::class.java)
//            )
//        ).thenReturn(emptyList()) // READY 없음
//
//        // when
//        paymentScheduler.reconcilePayments()
//
//        // then
//        // 상태 변동 없음 → save 호출 안 될 수 있음
//        verify(paymentRepository, never()).save(inProgress)
//        assertEquals(PaymentStatus.IN_PROGRESS, inProgress.paymentStatus)
//    }
//}