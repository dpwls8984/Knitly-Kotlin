package com.mysite.knitly.domain.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.payment.client.TossApiClient;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.payment.scheduler.PaymentScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSchedulerTest {

    @InjectMocks
    private PaymentScheduler paymentScheduler;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock private TossApiClient tossApiClient;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void reconcilePayments_inProgressToDone_and_readyToCanceled() throws Exception {
        // ----- IN_PROGRESS 복구 대상 -----
        Payment inProgress = Payment.builder()
                .paymentId(1L)
                .tossPaymentKey("pay_1")
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .requestedAt(LocalDateTime.now().minusMinutes(20))
                .totalAmount(12345L)
                .build();

        when(paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS), any(LocalDateTime.class))
        ).thenReturn(List.of(inProgress));

        JsonNode done = om.readTree("""
          {"status":"DONE","method":"CARD","approvedAt":"2025-11-10T03:10:00Z"}
        """);
        when(tossApiClient.queryPayment("pay_1")).thenReturn(done);

        // ----- READY 취소 대상 -----
        Payment ready = Payment.builder()
                .paymentId(2L)
                .paymentStatus(PaymentStatus.READY)
                .requestedAt(LocalDateTime.now().minusMinutes(40))
                .build();

        when(paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.READY), any(LocalDateTime.class))
        ).thenReturn(List.of(ready));

        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        paymentScheduler.reconcilePayments();

        // then
        // IN_PROGRESS → DONE 저장, READY → CANCELED 저장
        verify(paymentRepository, atLeast(2)).save(any(Payment.class));
        assertEquals(PaymentStatus.DONE, inProgress.getPaymentStatus());
        assertEquals(PaymentStatus.CANCELED, ready.getPaymentStatus());
    }

    @Test
    void reconcilePayments_inProgressCanceledOnToss() throws Exception {
        // given
        Payment inProgress = Payment.builder()
                .paymentId(3L)
                .tossPaymentKey("pay_3")
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .requestedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS), any(LocalDateTime.class))
        ).thenReturn(List.of(inProgress));

        JsonNode canceled = om.readTree("""
                {"status":"CANCELED"}
                """);
        when(tossApiClient.queryPayment("pay_3")).thenReturn(canceled);
        when(paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.READY), any(LocalDateTime.class))
        ).thenReturn(List.of()); // READY 없음

        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        paymentScheduler.reconcilePayments();

        // then
        assertEquals(PaymentStatus.FAILED, inProgress.getPaymentStatus()); // payment.fail(...) 도메인에서 FAILED로 바뀐다고 가정
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void reconcilePayments_inProgressStillInProgress() throws Exception {
        // given
        Payment inProgress = Payment.builder()
                .paymentId(4L)
                .tossPaymentKey("pay_4")
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .requestedAt(LocalDateTime.now().minusMinutes(25)).build();

        when(paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.IN_PROGRESS), any(LocalDateTime.class))
        ).thenReturn(List.of(inProgress));

        JsonNode still = om.readTree("""
                {"status":"IN_PROGRESS"}
                """);
        when(tossApiClient.queryPayment("pay_4")).thenReturn(still);
        when(paymentRepository.findByPaymentStatusAndRequestedAtBefore(
                eq(PaymentStatus.READY), any(LocalDateTime.class))
        ).thenReturn(List.of()); // READY 없음

        // when
        paymentScheduler.reconcilePayments();

        // then
        // 상태 변동 없음 → save 호출 안 될 수 있음
        verify(paymentRepository, never()).save(inProgress);
        assertEquals(PaymentStatus.IN_PROGRESS, inProgress.getPaymentStatus());
    }
}
