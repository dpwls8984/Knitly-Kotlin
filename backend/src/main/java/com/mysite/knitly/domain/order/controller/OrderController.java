package com.mysite.knitly.domain.order.controller;

import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.dto.OrderCreateResponse;
import com.mysite.knitly.domain.order.service.OrderFacade;
import com.mysite.knitly.domain.order.service.OrderService;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderFacade orderFacade;
    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid OrderCreateRequest request
    ) {
        // ✅ Facade의 메서드를 호출하도록 변경
        OrderCreateResponse response = orderFacade.createOrderWithLock(user, request);
        return ResponseEntity.ok(response);
    }
}
