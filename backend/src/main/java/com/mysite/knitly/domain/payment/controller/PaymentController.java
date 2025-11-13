package com.mysite.knitly.domain.payment.controller;

import com.mysite.knitly.domain.payment.dto.PaymentCancelRequest;
import com.mysite.knitly.domain.payment.dto.PaymentCancelResponse;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmResponse;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    //토스페이먼츠 결제 승인
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @RequestBody @Valid PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    // 결제 취소
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentCancelResponse> cancelPayment(
            @PathVariable Long paymentId,
            @RequestBody @Valid PaymentCancelRequest request
    ) {
        PaymentCancelResponse response = paymentService.cancelPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 토스페이먼츠 웹훅
     * 결제 상태 변경 시 토스가 호출
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[PaymentController] 웹훅 수신 - payload={}", payload);

        try {
            paymentService.handleWebhook(payload);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("[PaymentController] 웹훅 처리 실패", e);
            // 토스는 200 응답을 받아야 재전송 안 함
            // 실패해도 200 반환하고 로그만 남김
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 결제 상태 조회
     * - 프론트엔드에서 결제 상태 확인 시 호출
     * - 마이페이지 결제 내역 조회에서 사용
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<Map<String, String>> getPaymentStatus(
            @PathVariable Long paymentId
    ) {
        log.info("[PaymentController] 결제 상태 조회 - paymentId={}", paymentId);

        PaymentStatus status = paymentService.queryPaymentStatus(paymentId);

        return ResponseEntity.ok(Map.of(
                "paymentId", paymentId.toString(),
                "status", status.name()
        ));
    }
}
