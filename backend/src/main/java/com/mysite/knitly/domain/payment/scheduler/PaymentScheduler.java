package com.mysite.knitly.domain.payment.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysite.knitly.domain.payment.client.TossApiClient;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 미완료 결제 복구 스케줄러
 * - 결제 완료 후 창 닫기/네트워크 끊김으로 인한 IN_PROGRESS 상태 결제를 복구
 * - READY 상태에서 오래 방치된 결제를 CANCELED 처리
 * - 매 5분마다 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentRepository paymentRepository;
    private final TossApiClient tossApiClient;

    /**
     * 매 5분마다 실행
     * 1. IN_PROGRESS 상태 결제 복구
     * 2. READY 상태 결제 취소 처리
     */
    @Scheduled(fixedDelay = 300000) // 5분 (300,000ms)
    @Transactional
    public void reconcilePayments() {
        long startTime = System.currentTimeMillis();
        log.info("[Payment] [Scheduler] 결제 복구 스케줄러 시작");

        try {
            // 1. IN_PROGRESS 복구
            reconcileInProgressPayments();

            // 2. READY 취소 처리
            cancelAbandonedReadyPayments();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Payment] [Scheduler] 결제 복구 스케줄러 완료 - duration={}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Scheduler] 스케줄러 실행 중 오류 발생 - duration={}ms", duration, e);
        }
    }

    /**
     * IN_PROGRESS 상태인 결제를 토스에서 조회하여 동기화
     * - 10분 이상 IN_PROGRESS 상태로 남아있는 결제들을 대상으로 함
     * - 토스에서 완료된 결제는 DONE으로 업데이트
     * - 토스에서 실패/취소된 결제는 FAILED로 업데이트
     */
    private void reconcileInProgressPayments() {
        long startTime = System.currentTimeMillis();
        log.info("[Payment] [Reconciliation] IN_PROGRESS 결제 복구 시작");

        try {
            // 10분 이상 IN_PROGRESS 상태인 결제들 조회
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
            List<Payment> inProgressPayments = paymentRepository
                    .findByPaymentStatusAndRequestedAtBefore(
                            PaymentStatus.IN_PROGRESS,
                            threshold
                    )
                    .stream()
                    .limit(100)  // 한 번에 최대 100건만
                    .toList();

            if (inProgressPayments.isEmpty()) {
                log.info("[Payment] [Reconciliation] IN_PROGRESS 복구 대상 없음");
                return;
            }

            log.info("[Payment] [Reconciliation] IN_PROGRESS 복구 대상: {}건", inProgressPayments.size());

            int successCount = 0;
            int failedCount = 0;
            int errorCount = 0;

            for (Payment payment : inProgressPayments) {
                try {
                    boolean reconciled = reconcilePayment(payment);
                    if (reconciled) {
                        if (payment.getPaymentStatus() == PaymentStatus.DONE) {
                            successCount++;
                        } else {
                            failedCount++;
                        }
                    }

                } catch (Exception e) {
                    errorCount++;
                    log.error("[Payment] [Reconciliation] 복구 실패 - paymentId={}, error={}",
                            payment.getPaymentId(), e.getMessage(), e);
                    // 한 건 실패해도 다음 건 계속 처리
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Payment] [Reconciliation] IN_PROGRESS 복구 완료 - 대상={}건, 성공={}건, 실패={}건, 오류={}건, duration={}ms",
                    inProgressPayments.size(), successCount, failedCount, errorCount, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Reconciliation] IN_PROGRESS 복구 중 오류 - duration={}ms", duration, e);
        }
    }

    /**
     * 개별 결제 복구 처리
     */
    private boolean reconcilePayment(Payment payment) {
        String paymentKey = payment.getTossPaymentKey();
        Long paymentId = payment.getPaymentId();

        log.info("[Payment] [Reconciliation] 복구 시도 - paymentId={}, paymentKey={}, requestedAt={}",
                paymentId, paymentKey, payment.getRequestedAt());

        try {
            // 토스에서 실제 결제 상태 조회
            JsonNode tossResponse = tossApiClient.queryPayment(paymentKey);

            if (tossResponse == null) {
                log.warn("[Payment] [Reconciliation] 토스 응답 없음 - paymentId={}", paymentId);
                return false;
            }

            String tossStatus = tossResponse.has("status") ?
                    tossResponse.get("status").asText() : null;

            if (tossStatus == null) {
                log.warn("[Payment] [Reconciliation] 토스 상태 정보 없음 - paymentId={}", paymentId);
                return false;
            }

            log.info("[Payment] [Reconciliation] 토스 상태 확인 - paymentId={}, 현재={}, 토스={}",
                    paymentId, payment.getPaymentStatus(), tossStatus);

            // 토스에서 완료된 결제면 동기화
            if ("DONE".equals(tossStatus)) {
                updatePaymentFromTossResponse(payment, tossResponse);
                paymentRepository.save(payment);

                log.info("[Payment] [Reconciliation] 결제 복구 완료 - paymentId={}, status=DONE, amount={}",
                        paymentId, payment.getTotalAmount());
                return true;
            }
            // 토스에서 실패/취소되었으면 FAILED로 변경
            else if ("CANCELED".equals(tossStatus) || "FAILED".equals(tossStatus)) {
                payment.fail("토스 상태: " + tossStatus);
                paymentRepository.save(payment);

                log.warn("[Payment] [Reconciliation] 결제 실패 처리 - paymentId={}, tossStatus={}",
                        paymentId, tossStatus);
                return true;
            }
            // 토스에서도 IN_PROGRESS면 아직 처리 중
            else if ("IN_PROGRESS".equals(tossStatus) || "WAITING_FOR_DEPOSIT".equals(tossStatus)) {
                log.info("[Payment] [Reconciliation] 토스에서도 처리 중 - paymentId={}, tossStatus={}",
                        paymentId, tossStatus);
                return false;
            }
            // 알 수 없는 상태
            else {
                log.warn("[Payment] [Reconciliation] 알 수 없는 토스 상태 - paymentId={}, tossStatus={}",
                        paymentId, tossStatus);
                return false;
            }

        } catch (Exception e) {
            log.error("[Payment] [Reconciliation] 복구 중 예외 발생 - paymentId={}", paymentId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * READY 상태에서 30분 이상 방치된 결제 취소 처리
     */
    private void cancelAbandonedReadyPayments() {
        long startTime = System.currentTimeMillis();
        log.info("[Payment] [Reconciliation] READY 결제 취소 시작");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
            List<Payment> readyPayments = paymentRepository
                    .findByPaymentStatusAndRequestedAtBefore(
                            PaymentStatus.READY,
                            threshold
                    );

            if (readyPayments.isEmpty()) {
                log.info("[Payment] [Reconciliation] READY 취소 대상 없음");
                return;
            }

            log.info("[Payment] [Reconciliation] READY 취소 대상: {}건", readyPayments.size());

            int canceledCount = 0;

            for (Payment payment : readyPayments) {
                try {
                    payment.cancel("결제 위젯에서 30분간 미진행");
                    paymentRepository.save(payment);

                    log.info("[Payment] [Reconciliation] READY → CANCELED 처리 - paymentId={}, orderId={}",
                            payment.getPaymentId(), payment.getOrder().getOrderId());

                    canceledCount++;

                } catch (Exception e) {
                    log.error("[Payment] [Reconciliation] READY 취소 실패 - paymentId={}",
                            payment.getPaymentId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Payment] [Reconciliation] READY 취소 완료 - {}건, duration={}ms", canceledCount, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Reconciliation] READY 취소 중 오류 - duration={}ms", duration, e);
        }
    }

    private void updatePaymentFromTossResponse(Payment payment, JsonNode tossResponse) {
        String method = tossResponse.get("method").asText();
        String status = tossResponse.get("status").asText();

        payment.setPaymentMethod(PaymentMethod.fromString(method));
        payment.setPaymentStatus(PaymentStatus.fromString(status));

        if (tossResponse.has("mId")) {
            payment.setMid(tossResponse.get("mId").asText());
        }

        if (tossResponse.has("approvedAt")) {
            String approvedAtStr = tossResponse.get("approvedAt").asText();
            LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr,
                    DateTimeFormatter.ISO_DATE_TIME);
            payment.approve(approvedAt);
        }
    }
}