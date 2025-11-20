package com.mysite.knitly.domain.payment.controller

import PaymentConfirmResponse
import com.mysite.knitly.domain.payment.dto.PaymentCancelRequest
import com.mysite.knitly.domain.payment.dto.PaymentCancelResponse
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest
import com.mysite.knitly.domain.payment.service.PaymentService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val paymentService: PaymentService
) {

    /**
     * 토스페이먼츠 결제 승인
     */
    @PostMapping("/confirm")
    fun confirmPayment(
        @Valid @RequestBody request: PaymentConfirmRequest
    ): ResponseEntity<PaymentConfirmResponse> =
        ResponseEntity.ok(paymentService.confirmPayment(request))

    /**
     * 결제 취소
     */
    @PostMapping("/{paymentId}/cancel")
    fun cancelPayment(
        @PathVariable paymentId: Long,
        @Valid @RequestBody request: PaymentCancelRequest
    ): ResponseEntity<PaymentCancelResponse> =
        ResponseEntity.ok(paymentService.cancelPayment(paymentId, request))

    /**
     * 토스페이먼츠 웹훅
     * 결제 상태 변경 시 토스가 호출
     */
    @PostMapping("/webhook")
    fun handleWebhook(@RequestBody payload: Map<String, Any>): ResponseEntity<Void> {
        log.info("[PaymentController] 웹훅 수신 - payload={}", payload)

        return try {
            paymentService.handleWebhook(payload)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            log.error("[PaymentController] 웹훅 처리 실패", e)
            // 토스는 200 응답을 받아야 재전송 안 함
            // 실패해도 200 반환하고 로그만 남김
            ResponseEntity.ok().build()
        }
    }

    /**
     * 결제 상태 조회
     * - 프론트엔드에서 결제 상태 확인 시 호출
     * - 마이페이지 결제 내역 조회에서 사용
     */
    @GetMapping("/{paymentId}/status")
    fun getPaymentStatus(@PathVariable paymentId: Long): ResponseEntity<Map<String, String>> {
        log.info("[PaymentController] 결제 상태 조회 - paymentId={}", paymentId)

        val status = paymentService.queryPaymentStatus(paymentId)

        return ResponseEntity.ok(
            mapOf(
                "paymentId" to paymentId.toString(),
                "status" to status.name
            )
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentController::class.java)
    }
}
