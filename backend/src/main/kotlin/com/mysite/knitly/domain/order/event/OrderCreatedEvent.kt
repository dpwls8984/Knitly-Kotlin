package com.mysite.knitly.domain.order.event

import com.mysite.knitly.domain.product.product.entity.Product

data class OrderCreatedEvent(
    val orderedProducts: List<Product>
)
