package com.mysite.knitly.domain.order.controller;

import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.dto.OrderCreateResponse;
import com.mysite.knitly.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
            // TODO: JWT 인증 적용 후 @AuthenticationPrincipal로 변경
            @PathVariable("userId") UUID userId,
            @RequestBody @Valid OrderCreateRequest request
    ) {
        OrderCreateResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.ok(response);
    }
}
