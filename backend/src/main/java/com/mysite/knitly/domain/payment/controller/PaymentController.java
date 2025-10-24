package com.mysite.knitly.domain.payment.controller;

import com.mysite.knitly.domain.payment.dto.PaymentCancelRequest;
import com.mysite.knitly.domain.payment.dto.PaymentCancelResponse;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmResponse;
import com.mysite.knitly.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
