package com.mysite.knitly.domain.order.event;

import com.mysite.knitly.domain.product.product.entity.Product;

import java.util.List;

public record OrderCreatedEvent(
        List<Product> orderedProducts
) {
}
