package com.mysite.knitly.domain.order.controller

import com.mysite.knitly.domain.order.dto.OrderCreateRequest
import com.mysite.knitly.domain.order.dto.OrderCreateResponse
import com.mysite.knitly.domain.order.service.OrderFacade
import com.mysite.knitly.domain.user.entity.User
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderFacade: OrderFacade
) {
    @PostMapping
    fun createOrder(
        @AuthenticationPrincipal user: User,
        @RequestBody @Valid request: OrderCreateRequest
    ): ResponseEntity<OrderCreateResponse> {
        val response = orderFacade.createOrderWithLock(user, request)
        return ResponseEntity.ok(response)
    }
}