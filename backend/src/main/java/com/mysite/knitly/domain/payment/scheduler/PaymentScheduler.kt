package com.mysite.knitly.domain.payment.scheduler

import com.fasterxml.jackson.databind.JsonNode
import com.mysite.knitly.domain.payment.client.TossApiClient
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 미완료 결제 복구 스케줄러
 * - 결제 완료 후 창 닫기/네트워크 끊김으로 인한 IN_PROGRESS 상태 결제를 복구
 * - READY 상태에서 오래 방치된 결제를 CANCELED 처리
 * - 매 5분마다 실행
 */
@Component
class PaymentScheduler(
    private val paymentRepository: PaymentRepository,
    private val tossApiClient: TossApiClient
) {

    /**
     * 매 5분마다 실행
     * 1. IN_PROGRESS 상태 결제 복구
     * 2. READY 상태 결제 취소 처리
     */
    @Scheduled(fixedDelay = 300000) // 5분 (300,000ms)
    @Transactional
    fun reconcilePayments() {
        val startTime = System.currentTimeMillis()
        log.info("[Payment] [Scheduler] 결제 복구 스케줄러 시작")

        try {
            // 1. IN_PROGRESS 복구
            reconcileInProgressPayments()

            // 2. READY 취소 처리
            cancelAbandonedReadyPayments()

            val duration = System.currentTimeMillis() - startTime
            log.info("[Payment] [Scheduler] 결제 복구 스케줄러 완료 - duration={}ms", duration)

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Payment] [Scheduler] 스케줄러 실행 중 오류 발생 - duration={}ms", duration, e)
        }
    }

    /**
     * IN_PROGRESS 상태인 결제를 토스에서 조회하여 동기화
     * - 10분 이상 IN_PROGRESS 상태로 남아있는 결제들을 대상으로 함
     * - 토스에서 완료된 결제는 DONE으로 업데이트
     * - 토스에서 실패/취소된 결제는 FAILED로 업데이트
     */
    private fun reconcileInProgressPayments() {
        val startTime = System.currentTimeMillis()
        log.info("[Payment] [Reconciliation] IN_PROGRESS 결제 복구 시작")

        try {
            // 10분 이상 IN_PROGRESS 상태인 결제들 조회
            val threshold = LocalDateTime.now().minusMinutes(10)
            val inProgressPayments = paymentRepository
                .findByPaymentStatusAndRequestedAtBefore(
                    PaymentStatus.IN_PROGRESS,
                    threshold
                )
                .take(100)  // 한 번에 최대 100건만

            if (inProgressPayments.isEmpty()) {
                log.info("[Payment] [Reconciliation] IN_PROGRESS 복구 대상 없음")
                return
            }

            log.info("[Payment] [Reconciliation] IN_PROGRESS 복구 대상: {}건", inProgressPayments.size)

            val results = inProgressPayments.map { payment ->
                try {
                    val reconciled = reconcilePayment(payment)
                    when {
                        !reconciled -> ReconcileResult.NOT_RECONCILED
                        payment.paymentStatus == PaymentStatus.DONE -> ReconcileResult.SUCCESS
                        else -> ReconcileResult.FAILED
                    }
                } catch (e: Exception) {
                    log.error(
                        "[Payment] [Reconciliation] 복구 실패 - paymentId={}, error={}",
                        payment.paymentId, e.message, e
                    )
                    ReconcileResult.ERROR
                }
            }

            val successCount = results.count { it == ReconcileResult.SUCCESS }
            val failedCount = results.count { it == ReconcileResult.FAILED }
            val errorCount = results.count { it == ReconcileResult.ERROR }

            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Payment] [Reconciliation] IN_PROGRESS 복구 완료 - 대상={}건, 성공={}건, 실패={}건, 오류={}건, duration={}ms",
                inProgressPayments.size, successCount, failedCount, errorCount, duration
            )

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Payment] [Reconciliation] IN_PROGRESS 복구 중 오류 - duration={}ms", duration, e)
        }
    }

    /**
     * 개별 결제 복구 처리
     */
    private fun reconcilePayment(payment: Payment): Boolean {
        val paymentKey = payment.tossPaymentKey ?: run {
            log.warn("[Payment] [Reconciliation] paymentKey가 없음 - paymentId={}", payment.paymentId)
            return false
        }
        val paymentId = payment.paymentId

        log.info(
            "[Payment] [Reconciliation] 복구 시도 - paymentId={}, paymentKey={}, requestedAt={}",
            paymentId, paymentKey, payment.requestedAt
        )

        try {
            // 토스에서 실제 결제 상태 조회
            val tossResponse = tossApiClient.queryPayment(paymentKey)

            val tossStatus = tossResponse.get("status")?.asText() ?: run {
                log.warn("[Payment] [Reconciliation] 토스 상태 정보 없음 - paymentId={}", paymentId)
                return false
            }

            log.info(
                "[Payment] [Reconciliation] 토스 상태 확인 - paymentId={}, 현재={}, 토스={}",
                paymentId, payment.paymentStatus, tossStatus
            )

            // 토스 상태에 따라 처리
            return when (tossStatus) {
                "DONE" -> {
                    updatePaymentFromTossResponse(payment, tossResponse)
                    paymentRepository.save(payment)
                    log.info(
                        "[Payment] [Reconciliation] 결제 복구 완료 - paymentId={}, status=DONE, amount={}",
                        paymentId, payment.totalAmount
                    )
                    true
                }
                "CANCELED", "FAILED" -> {
                    payment.fail("토스 상태: $tossStatus")
                    paymentRepository.save(payment)
                    log.warn(
                        "[Payment] [Reconciliation] 결제 실패 처리 - paymentId={}, tossStatus={}",
                        paymentId, tossStatus
                    )
                    true
                }
                "IN_PROGRESS", "WAITING_FOR_DEPOSIT" -> {
                    log.info(
                        "[Payment] [Reconciliation] 토스에서도 처리 중 - paymentId={}, tossStatus={}",
                        paymentId, tossStatus
                    )
                    false
                }
                else -> {
                    log.warn(
                        "[Payment] [Reconciliation] 알 수 없는 토스 상태 - paymentId={}, tossStatus={}",
                        paymentId, tossStatus
                    )
                    false
                }
            }

        } catch (e: Exception) {
            log.error("[Payment] [Reconciliation] 복구 중 예외 발생 - paymentId={}", paymentId, e)
            throw RuntimeException(e)
        }
    }

    /**
     * READY 상태에서 30분 이상 방치된 결제 취소 처리
     */
    private fun cancelAbandonedReadyPayments() {
        val startTime = System.currentTimeMillis()
        log.info("[Payment] [Reconciliation] READY 결제 취소 시작")

        try {
            val threshold = LocalDateTime.now().minusMinutes(30)
            val readyPayments = paymentRepository
                .findByPaymentStatusAndRequestedAtBefore(
                    PaymentStatus.READY,
                    threshold
                )

            if (readyPayments.isEmpty()) {
                log.info("[Payment] [Reconciliation] READY 취소 대상 없음")
                return
            }

            log.info("[Payment] [Reconciliation] READY 취소 대상: {}건", readyPayments.size)

            val canceledCount = readyPayments.count { payment ->
                try {
                    payment.cancel("결제 위젯에서 30분간 미진행")
                    paymentRepository.save(payment)
                    log.info(
                        "[Payment] [Reconciliation] READY → CANCELED 처리 - paymentId={}, orderId={}",
                        payment.paymentId, payment.order.orderId
                    )
                    true
                } catch (e: Exception) {
                    log.error(
                        "[Payment] [Reconciliation] READY 취소 실패 - paymentId={}",
                        payment.paymentId, e
                    )
                    false
                }
            }

            val duration = System.currentTimeMillis() - startTime
            log.info("[Payment] [Reconciliation] READY 취소 완료 - {}건, duration={}ms", canceledCount, duration)

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Payment] [Reconciliation] READY 취소 중 오류 - duration={}ms", duration, e)
        }
    }

    private fun updatePaymentFromTossResponse(payment: Payment, tossResponse: JsonNode) {
        val method = tossResponse.get("method").asText()
        val status = tossResponse.get("status").asText()

        payment.paymentMethod = PaymentMethod.fromString(method)
        payment.paymentStatus = PaymentStatus.fromString(status)

        tossResponse.get("mId")?.asText()?.let { payment.mid = it }

        tossResponse.get("approvedAt")?.asText()
            ?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
            ?.let { payment.approve(it) }
    }

    /**
     * 복구 결과를 나타내는 enum
     */
    private enum class ReconcileResult {
        SUCCESS,        // 복구 성공 (DONE)
        FAILED,         // 복구 성공했지만 실패/취소 상태
        NOT_RECONCILED, // 아직 처리 중
        ERROR           // 복구 중 오류
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentScheduler::class.java)
    }
}